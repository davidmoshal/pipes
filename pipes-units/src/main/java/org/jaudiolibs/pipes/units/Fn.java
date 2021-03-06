/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2019 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 */
package org.jaudiolibs.pipes.units;

import java.util.List;
import java.util.function.DoubleUnaryOperator;
import org.jaudiolibs.pipes.Buffer;
import org.jaudiolibs.pipes.Pipe;

/**
 * A single channel unit that processes each sample with a function.
 */
public final class Fn extends Pipe {
    
    private final static DoubleUnaryOperator PASSTHROUGH = d -> d;

    private DoubleUnaryOperator fn;
    
    /**
     * Create a Fn unit with a default pass through (no-op) function.
     */
    public Fn() {
        super(1,1);
    }
    
    /**
     * Create a Fn unit with the provided function.
     * 
     * @param fn function to process each sample
     */
    public Fn(DoubleUnaryOperator fn) {
        this();
        function(fn);
    }
    
    /**
     * Set the function of this unit.
     * 
     * @param fn function to process each sample
     * @return this
     */
    public Fn function(DoubleUnaryOperator fn) {
        this.fn = fn == null ? PASSTHROUGH : fn;
        return this;
    }
    
    /**
     * Reset the function to the default no-op pass through.
     */
    @Override
    public void reset() {
        this.fn = null;
    }

    @Override
    protected void process(List<Buffer> buffers) {
        if (buffers.isEmpty()) {
            return; // should never happen!
        }
        DoubleUnaryOperator f = fn == null ? PASSTHROUGH : fn;
        Buffer buffer = buffers.get(0);
        int size = buffer.getSize();
        float[] data = buffer.getData();
        for (int i=0; i < size; i++) {
            data[i] = (float) f.applyAsDouble(data[i]);
        }
    }
    
    
    
}
