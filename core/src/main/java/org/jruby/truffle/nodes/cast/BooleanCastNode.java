/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

import java.math.BigInteger;

/**
 * Casts a value into a boolean.
 */
@NodeChild(value = "child", type = RubyNode.class)
public abstract class BooleanCastNode extends RubyNode {

    public BooleanCastNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public BooleanCastNode(BooleanCastNode copy) {
        super(copy.getContext(), copy.getSourceSection());
    }

    @Specialization
    public boolean doNil(@SuppressWarnings("unused") RubyNilClass nil) {
        return false;
    }

    @Specialization
    public boolean doFalse(@SuppressWarnings("unused") RubyFalseClass falseObject) {
        return false;
    }

    @Specialization
    public boolean doBoolean(boolean value) {
        return value;
    }

    @Specialization
    public boolean doIntegerFixnum(int value) {
        return true;
    }

    @Specialization
    public boolean doLongFixnum(long value) {
        return true;
    }

    @Specialization
    public boolean doBignum(BigInteger value) {
        return true;
    }

    @Specialization
    public boolean doFloat(double value) {
        return true;
    }

    @Specialization(guards = "neitherNilNorFalse")
    public boolean doBasicObject(RubyBasicObject object) {
        return true;
    }

    protected boolean neitherNilNorFalse(RubyBasicObject object) {
        return object != getContext().getCoreLibrary().getNilObject() &&
                object != getContext().getCoreLibrary().getFalseObject();
    }

    @Override
    public abstract boolean executeBoolean(VirtualFrame frame);

    public abstract boolean executeBoolean(VirtualFrame frame, Object value);

}
