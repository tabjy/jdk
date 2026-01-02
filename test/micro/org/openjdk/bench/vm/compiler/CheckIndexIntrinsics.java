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

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2)
public class CheckIndexIntrinsics {

    @Param({"64"})
    private int batchSize;

    private static class UnintrinsifiedBondCheckBuffer {
        private byte[] buffer = new byte[1024];

        public byte getByte(int pos) {
            checkUpperBound(pos, 1);
            return _getByte(pos);
        }

        private void checkUpperBound(int index, int size) {
            int length = buffer.length;
            if ((index | length - (index + size)) < 0) { // vertx
                throw new RuntimeException("oob");
            }
        }

        private byte _getByte(int pos) {
            checkIndex(pos, 1);
            return buffer[pos];
        }

        private void checkIndex(int index, int size) {
            unintrinsifiedCheckFromIndexSize(index, size, buffer.length);
        }

        // Mimicking non-intrinsified bytecode from as in jdk.internal.util.Preconditions
        private static int unintrinsifiedCheckFromIndexSize(int fromIndex, int size, int length) {
            if ((length | fromIndex | size) < 0 || size > length - fromIndex) // netty
                throw new RuntimeException("oob");
            return fromIndex;
        }
    }

    private static class IntrinsifiedBondCheckBuffer {
        private byte[] buffer = new byte[1024];

        public byte getByte(int pos) {
            checkUpperBound(pos, 1); // vertx
            return _getByte(pos);
        }

        private void checkUpperBound(int index, int size) {
            Objects.checkFromIndexSize(index, size, buffer.length);
        }

        private byte _getByte(int pos) {
            checkIndex(pos, 1); // netty
            return buffer[pos];
        }

        private void checkIndex(int index, int size) {
            Objects.checkFromIndexSize(index, size, buffer.length);
        }
    }

    @Benchmark
    public void getByteBatchUnintrinsified(Blackhole bh) {
        UnintrinsifiedBondCheckBuffer buffer = new UnintrinsifiedBondCheckBuffer();
        for (int i = 0, size = batchSize; i < size; i++) {
            bh.consume(buffer.getByte(i)); // TODO:
        }
    }

    @Benchmark
    public void getByteBatchIntrinsified(Blackhole bh) {
        IntrinsifiedBondCheckBuffer buffer = new IntrinsifiedBondCheckBuffer();
        for (int i = 0, size = batchSize; i < size; i++) {
            bh.consume(buffer.getByte(i));
        }
    }
}
