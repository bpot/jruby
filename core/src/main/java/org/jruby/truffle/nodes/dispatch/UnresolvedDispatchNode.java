/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.RubyMethod;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.util.cli.Options;

import java.io.PrintStream;

public final class UnresolvedDispatchNode extends DispatchNode {

    private int depth = 0;

    private final boolean ignoreVisibility;
    private final boolean indirect;
    private final Dispatch.MissingBehavior missingBehavior;

    public UnresolvedDispatchNode(RubyContext context, boolean ignoreVisibility, boolean indirect,
                                  Dispatch.MissingBehavior missingBehavior) {
        super(context);
        this.ignoreVisibility = ignoreVisibility;
        this.indirect = indirect;
        this.missingBehavior = missingBehavior;
    }

    @Override
    public Object executeDispatch(
            VirtualFrame frame,
            Object methodReceiverObject,
            LexicalScope lexicalScope,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        if (depth == Options.TRUFFLE_DISPATCH_POLYMORPHIC_MAX.load()) {
            return getHeadNode().getFirstDispatchNode()
                    .replace(UncachedDispatchNodeFactory.create(getContext(), ignoreVisibility,
                            null, null, null, null, null, null, null))
                    .executeDispatch(frame, methodReceiverObject, lexicalScope, receiverObject,
                            methodName, blockObject, argumentsObjects, dispatchAction);
        }

        depth++;

        final DispatchNode first = getHeadNode().getFirstDispatchNode();

        if (receiverObject instanceof RubyBasicObject) {
            return doRubyBasicObject(
                    frame,
                    first,
                    (LexicalScope) lexicalScope,
                    receiverObject,
                    methodName,
                    blockObject,
                    argumentsObjects,
                    dispatchAction,
                    methodReceiverObject);
        } else {
            return doUnboxedObject(
                    frame,
                    first,
                    (LexicalScope) lexicalScope,
                    receiverObject,
                    methodName,
                    blockObject,
                    argumentsObjects,
                    dispatchAction,
                    methodReceiverObject);
        }
    }

    private Object doUnboxedObject(
            VirtualFrame frame,
            DispatchNode first,
            LexicalScope lexicalScope,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction,
            Object methodReceiverObject) {
        final RubyBasicObject boxedReceiverObject = getContext().getCoreLibrary().box(receiverObject);
        final RubyClass callerClass = ignoreVisibility ? null : getContext().getCoreLibrary().box(RubyArguments.getSelf(frame.getArguments())).getMetaClass();

        if (dispatchAction == Dispatch.DispatchAction.CALL_METHOD || dispatchAction == Dispatch.DispatchAction.RESPOND_TO_METHOD) {
            final RubyMethod method = lookup(callerClass, boxedReceiverObject, methodName.toString(), ignoreVisibility,
                    dispatchAction);

            if (method == null) {
                final DispatchNode newDispatch = createMethodMissingNode(methodName, boxedReceiverObject, dispatchAction);
                return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, boxedReceiverObject,
                        methodName, blockObject, argumentsObjects, dispatchAction);
            }

            if (receiverObject instanceof Boolean) {
                final Assumption falseUnmodifiedAssumption =
                        getContext().getCoreLibrary().getFalseClass().getUnmodifiedAssumption();

                final RubyMethod falseMethod =
                        lookup(callerClass, getContext().getCoreLibrary().box(false), methodName.toString(),
                                ignoreVisibility, dispatchAction);

                if (falseMethod == null) {
                    throw new UnsupportedOperationException();
                }

                final Assumption trueUnmodifiedAssumption =
                        getContext().getCoreLibrary().getTrueClass().getUnmodifiedAssumption();

                final RubyMethod trueMethod =
                        lookup(callerClass, getContext().getCoreLibrary().box(true), methodName.toString(),
                                ignoreVisibility, dispatchAction);

                if (trueMethod == null) {
                    throw new UnsupportedOperationException();
                }

                final CachedBooleanDispatchNode newDispatch = CachedBooleanDispatchNodeFactory.create(getContext(),
                        methodName, first,
                        falseUnmodifiedAssumption, null, falseMethod,
                        trueUnmodifiedAssumption, null, trueMethod, indirect,
                        null, null, null, null, null, null, null);

                first.replace(newDispatch);

                return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, receiverObject,
                        methodName, blockObject, argumentsObjects, dispatchAction);
            } else {
                final CachedUnboxedDispatchNode newDispatch = CachedUnboxedDispatchNodeFactory.create(getContext(),
                        methodName, first, receiverObject.getClass(),
                        boxedReceiverObject.getLogicalClass().getUnmodifiedAssumption(), null, method, indirect, null, null, null, null,
                        null, null, null);

                first.replace(newDispatch);

                return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, receiverObject, methodName,
                        blockObject, argumentsObjects, dispatchAction);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Object doRubyBasicObject(
            VirtualFrame frame,
            DispatchNode first,
            LexicalScope lexicalScope,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction,
            Object methodReceiverObject) {
        final RubyBasicObject boxedReceiverObject = getContext().getCoreLibrary().box(receiverObject);
        final RubyClass callerClass = ignoreVisibility ? null : getContext().getCoreLibrary().box(RubyArguments.getSelf(frame.getArguments())).getMetaClass();

        if (dispatchAction == Dispatch.DispatchAction.CALL_METHOD || dispatchAction == Dispatch.DispatchAction.RESPOND_TO_METHOD) {
            final RubyMethod method = lookup(callerClass, boxedReceiverObject, methodName.toString(), ignoreVisibility,
                    dispatchAction);

            if (method == null) {
                final DispatchNode newDispatch = createMethodMissingNode(methodName, boxedReceiverObject, dispatchAction);
                return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, boxedReceiverObject,
                        methodName, blockObject, argumentsObjects, dispatchAction);
            }

            final DispatchNode newDispatch;

            if (receiverObject instanceof RubySymbol) {
                newDispatch = CachedBoxedSymbolDispatchNodeFactory.create(getContext(), methodName, first, null, method, indirect, null,
                        null, null, null, null, null, null);
            } else {
                newDispatch = CachedBoxedDispatchNodeFactory.create(getContext(), methodName, first,
                        boxedReceiverObject.getMetaClass(), null, method, indirect, null, null, null, null, null, null, null);
            }

            first.replace(newDispatch);
            return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, receiverObject,
                    methodName, blockObject, argumentsObjects, dispatchAction);

        } else if (dispatchAction == Dispatch.DispatchAction.READ_CONSTANT) {
            final RubyModule module = (RubyModule) boxedReceiverObject;
            final RubyConstant constant = lookupConstant(lexicalScope, module, methodName.toString(),
                    ignoreVisibility, dispatchAction);

            if (constant == null) {
                final DispatchNode newDispatch = createConstantMissingNode(methodName, callerClass, module, dispatchAction);
                return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, module,
                        methodName, blockObject, argumentsObjects, dispatchAction);
            }

            // The module, the "receiver" is an instance of its singleton class.
            // But we want to check the module assumption, not its singleton class assumption.
            final DispatchNode newDispatch = CachedBoxedDispatchNodeFactory.create(getContext(), methodName, first,
                    module.getSingletonClass(null), module.getUnmodifiedAssumption(), constant.getValue(),
                    null, indirect, null, null, null, null, null, null, null);

            first.replace(newDispatch);
            return newDispatch.executeDispatch(frame, methodReceiverObject, lexicalScope, receiverObject,
                    methodName, blockObject, argumentsObjects, dispatchAction);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private DispatchNode createConstantMissingNode(
            Object methodName,
            RubyClass callerClass,
            RubyBasicObject receiverObject,
            Dispatch.DispatchAction dispatchAction) {
        final DispatchNode first = getHeadNode().getFirstDispatchNode();

        switch (missingBehavior) {
            case RETURN_MISSING: {
                return first.replace(CachedBoxedReturnMissingDispatchNodeFactory.create(getContext(), methodName, first,
                        receiverObject.getMetaClass(), indirect, null, null, null, null, null, null, null));
            }

            case CALL_CONST_MISSING: {
                final RubyMethod method = lookup(callerClass, receiverObject, "const_missing", ignoreVisibility, dispatchAction);

                if (method == null) {
                    throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                            receiverObject.toString() + " didn't have a #const_missing", this));
                }

                if (Options.TRUFFLE_DISPATCH_METAPROGRAMMING_ALWAYS_UNCACHED.load()) {
                    return first.replace(UncachedDispatchNodeFactory.create(getContext(), ignoreVisibility,
                            null, null, null, null, null, null, null));
                }

                return first.replace(CachedBoxedMethodMissingDispatchNodeFactory.create(getContext(), methodName, first,
                        receiverObject.getMetaClass(), method, Options.TRUFFLE_DISPATCH_METAPROGRAMMING_ALWAYS_INDIRECT.load(), null, null, null, null, null, null, null));
            }

            default: {
                throw new UnsupportedOperationException(missingBehavior.toString());
            }
        }
    }

    private DispatchNode createMethodMissingNode(
            Object methodName,
            RubyBasicObject receiverObject,
            Dispatch.DispatchAction dispatchAction) {
        final DispatchNode first = getHeadNode().getFirstDispatchNode();

        switch (missingBehavior) {
            case RETURN_MISSING: {
                return first.replace(CachedBoxedReturnMissingDispatchNodeFactory.create(getContext(), methodName, first,
                        receiverObject.getMetaClass(), indirect, null, null, null, null, null, null, null));
            }

            case CALL_METHOD_MISSING: {
                final RubyMethod method = lookup(null, receiverObject, "method_missing", true, dispatchAction);

                if (method == null) {
                    throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                            receiverObject.toString() + " didn't have a #method_missing", this));
                }

                if (Options.TRUFFLE_DISPATCH_METAPROGRAMMING_ALWAYS_UNCACHED.load()) {
                    return first.replace(UncachedDispatchNodeFactory.create(getContext(), ignoreVisibility,
                                    null, null, null, null, null, null, null));
                }

                return first.replace(CachedBoxedMethodMissingDispatchNodeFactory.create(getContext(), methodName, first,
                        receiverObject.getMetaClass(), method, Options.TRUFFLE_DISPATCH_METAPROGRAMMING_ALWAYS_INDIRECT.load(), null, null, null, null, null, null, null));
            }

            default: {
                throw new UnsupportedOperationException(missingBehavior.toString());
            }
        }
    }

    private void dumpChain(PrintStream out) {
        out.println(getEncapsulatingSourceSection().getShortDescription());

        DispatchNode node = getHeadNode().getFirstDispatchNode();

        while (!(node instanceof UnresolvedDispatchNode)) {
            out.print("  ");
            out.println(node);

            if (node instanceof CachedDispatchNode) {
                node = ((CachedDispatchNode) node).next;
            } else {
                break;
            }
        }
    }

}
