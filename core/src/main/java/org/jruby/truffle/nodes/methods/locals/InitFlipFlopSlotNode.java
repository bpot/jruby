/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods.locals;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;

public class InitFlipFlopSlotNode extends RubyNode {

    private final FrameSlot frameSlot;

    public InitFlipFlopSlotNode(RubyContext context, SourceSection sourceSection, FrameSlot frameSlot) {
        super(context, sourceSection);
        this.frameSlot = frameSlot;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        frame.setBoolean(frameSlot, false);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        executeVoid(frame);
        return null;
    }

}
