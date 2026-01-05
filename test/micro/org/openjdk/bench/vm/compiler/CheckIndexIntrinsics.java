/*
 * Copyright (c) 2026, IBM and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.openjdk.bench.vm.compiler;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark to measure the performance impact of intrinsified checkFromIndexSize.
 *
 * This mimics the Vert.x BufferImpl pattern where:
 * - BufferImpl.getByte(pos) does ONE bounds check via checkUpperBound()
 * - Then delegates to Netty's ByteBuf.getByte() which does NO explicit bounds check
 *   (only the implicit JVM array bounds check)
 *
 * The key insight is that Vert.x has a SINGLE explicit bounds check per operation,
 * not multiple layered checks.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2)
public class CheckIndexIntrinsics {

    @Param({"64"})
    private int batchSize;

    // Buffers created in @Setup to match Vert.x benchmark structure
    private UnintrinsifiedBuffer unintrinsifiedBuffer;
    private IntrinsifiedBuffer intrinsifiedBuffer;

    @Setup
    public void setup() {
        // Match Vert.x: buffer size == batchSize (loop bound)
        unintrinsifiedBuffer = new UnintrinsifiedBuffer(batchSize);
        intrinsifiedBuffer = new IntrinsifiedBuffer(batchSize);

        // Initialize with data
        for (int i = 0; i < batchSize; i++) {
            unintrinsifiedBuffer.setByte(i, (byte) i);
            intrinsifiedBuffer.setByte(i, (byte) i);
        }
    }

    /**
     * Mimics Vert.x BufferImpl with unintrinsified bounds check.
     * Uses the same pattern as Vert.x: checkUpperBound() before array access.
     */
    private static class UnintrinsifiedBuffer {
        private final byte[] buffer;
        private int writerIndex;  // Mimics ByteBuf.writerIndex()

        UnintrinsifiedBuffer(int size) {
            buffer = new byte[size];
            writerIndex = size;
        }

        public byte getByte(int pos) {
            // Vert.x style: ONE bounds check, then direct array access
            checkUpperBound(pos, 1);
            return buffer[pos];  // Netty's HeapByteBufUtil.getByte() - no explicit check
        }

        public void setByte(int pos, byte b) {
            buffer[pos] = b;
            if (pos >= writerIndex) {
                writerIndex = pos + 1;
            }
        }

        // Mimics Vert.x BufferImpl.checkUpperBound - unintrinsified version
        private void checkUpperBound(int index, int size) {
            int length = writerIndex;  // writerIndex() call in real Vert.x
            if (index < 0 || index + size < 0 || index + size > length) {
                throw new IndexOutOfBoundsException(index + " + " + size + " > " + length);
            }
        }
    }

    /**
     * Mimics Vert.x BufferImpl with intrinsified bounds check.
     * Uses Objects.checkFromIndexSize() which should be intrinsified.
     */
    private static class IntrinsifiedBuffer {
        private final byte[] buffer;
        private int writerIndex;

        IntrinsifiedBuffer(int size) {
            buffer = new byte[size];
            writerIndex = size;
        }

        public byte getByte(int pos) {
            // Same pattern but using intrinsified check
            checkUpperBound(pos, 1);
            return buffer[pos];
        }

        public void setByte(int pos, byte b) {
            buffer[pos] = b;
            if (pos >= writerIndex) {
                writerIndex = pos + 1;
            }
        }

        // Uses intrinsified Objects.checkFromIndexSize
        private void checkUpperBound(int index, int size) {
            Objects.checkFromIndexSize(index, size, writerIndex);
        }
    }

    @Benchmark
    public void getByteBatchUnintrinsified(Blackhole bh) {
        UnintrinsifiedBuffer buffer = this.unintrinsifiedBuffer;
        for (int i = 0, size = batchSize; i < size; i++) {
            bh.consume(buffer.getByte(i));
        }
    }

    @Benchmark
    public void getByteBatchIntrinsified(Blackhole bh) {
        IntrinsifiedBuffer buffer = this.intrinsifiedBuffer;
        for (int i = 0, size = batchSize; i < size; i++) {
            bh.consume(buffer.getByte(i));
        }
    }
}
