/*
 * Copyright (c) 2022, IBM and/or its affiliates. All rights reserved.
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
package compiler.c2.irTests;

import compiler.lib.ir_framework.*;

import java.util.Objects;

/*
 * @test
 * @bug 8361837
 * @summary C2: investigate intrinsification of Preconditions.checkFromToIndex() and Preconditions.checkFromIndexSize()
 * @library /test/lib /
 * @requires vm.compiler2.enabled
 * @run driver compiler.c2.irTests.TestCheckIndexIntrinsics
 */
public class TestCheckIndexIntrinsics {
    public static void main(String[] args) {
        // TODO: remove PrintIdeal flag
        // TODO: w/wo -XX:ShortRunningLongLoop?
        TestFramework.runWithFlags("-XX:PrintIdealGraphLevel=2", "-XX:CompileOnly=compiler.c2.irTests.TestCheckIndexIntrinsics::*", "-XX:LoopMaxUnroll=0");
    }

    // Unintrinsified bytecode functions, as in jdk.internal.util.Preconditions
    private static int unintrinsifiedCheckIndex(int index, int length) {
        if (index < 0 || index >= length)
            throw new RuntimeException("oob");
        return index;
    }

    private static int unintrinsifiedCheckFromToIndex(int fromIndex, int toIndex, int length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length)
            throw new RuntimeException("oob");
        return fromIndex;
    }

    private static int unintrinsifiedCheckFromIndexSize(int fromIndex, int size, int length) {
        if ((length | fromIndex | size) < 0 || size > length - fromIndex)
            throw new RuntimeException("oob");
        return fromIndex;
    }

    // Corresponding long versions
    private static long unintrinsifiedCheckIndexL(long index, long length) {
        if (index < 0 || index >= length)
            throw new RuntimeException("oob");
        return index;
    }

    private static long unintrinsifiedCheckFromToIndexL(long fromIndex, long toIndex, long length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length)
            throw new RuntimeException("oob");
        return fromIndex;
    }

    private static long unintrinsifiedCheckFromIndexSizeL(long fromIndex, long size, long length) {
        if ((length | fromIndex | size) < 0 || size > length - fromIndex)
            throw new RuntimeException("oob");
        return fromIndex;
    }

    // TODO: re-enable tests
//    // Controlled test without intrinsics, should not have range checks (and traps) to begin with.
//    @Test
//    @IR(counts = { IRNode.COUNTED_LOOP, ">= 1"})
//    @IR(failOn = { IRNode.RANGE_CHECK_TRAP })
//    public static void testUnintrinsifiedCheckIndex(int start, int stop, int length, int offset) {
//        final int scale = 2;
//        final int stride = 1;
//
//        for (int i = start; i < stop; i += stride) {
//            unintrinsifiedCheckIndex(scale * i + offset, length);
//        }
//    }
//
//    // Same but for longs
//    @Test
//    @IR(counts = { IRNode.COUNTED_LOOP, ">= 1"})
//    @IR(failOn = { IRNode.RANGE_CHECK_TRAP })
//    public static void testUnintrinsifiedCheckIndexL(long start, long stop, long length, long offset) {
//        final long scale = 2;
//        final long stride = 1;
//
//        for (long i = start; i < stop; i += stride) {
//            unintrinsifiedCheckIndexL(scale * i + offset, length);
//        }
//    }
//
//    // Test range check (and trap) successfully eliminated
//    @Test
//    @IR(failOn = { IRNode.COUNTED_LOOP })
//    @IR(counts = { IRNode.RANGE_CHECK_TRAP, "1" }, phase = CompilePhase.AFTER_PARSING)
//    @IR(failOn = { IRNode.RANGE_CHECK_TRAP }) // phase = CompilePhase.BEFORE_MATCHING
//    public static void testCheckIndex(int start, int stop, int length, int offset) {
//        final int scale = 2;
//        final int stride = 1;
//
//        for (int i = start; i < stop; i += stride) {
//            Objects.checkIndex(scale * i + offset, length);
//        }
//    }
//
//    // Same but for longs
//    @Test
//    @IR(failOn = { IRNode.COUNTED_LOOP })
//    @IR(counts = { IRNode.RANGE_CHECK_TRAP, "1" }, phase = CompilePhase.AFTER_PARSING)
//    @IR(failOn = { IRNode.RANGE_CHECK_TRAP }) // phase = CompilePhase.BEFORE_MATCHING
//    public static void testCheckIndexL(long start, long stop, long length, long offset) {
//        final long scale = 2;
//        final long stride = 1;
//
//        for (long i = start; i < stop; i += stride) {
//            Objects.checkIndex(scale * i + offset, length);
//        }
//    }
//
//    @Run(test = {
//            "testUnintrinsifiedCheckIndex",
//            "testUnintrinsifiedCheckIndexL",
//            "testCheckIndex",
//            "testCheckIndexL"
//    })
//    private void testCheckIndex_runner() {
//        testUnintrinsifiedCheckIndex(0, 100, 200, 0);
//        testUnintrinsifiedCheckIndexL(0, 100, 200, 0);
//        testCheckIndex(0, 100, 200, 0);
//        testCheckIndexL(0, 100, 200, 0);
//    }

//     @Test
//     @IR(counts = { IRNode.COUNTED_LOOP, ">= 1"})
//     @IR(failOn = { IRNode.RANGE_CHECK_TRAP })
//     public static void testUnintrinsifiedCheckFromToIndex(int start, int stop, int length, int offset, int size) {
//         final int scale = 2;
//         final int stride = 1;
//
//         for (int i = start; i < stop; i += stride) {
//             int from = scale * i + offset;
//             int to = from + size;
//             unintrinsifiedCheckFromToIndex(from, to, length);
//         }
//     }

    @Test
    public static void testCheckFromIndexSize(int start, int stop, int length, int offset, int size) {
        final int scale = 2;
        final int stride = 1;

        for (int i = start; i < stop; i += stride) {
            int from = scale * i + offset;

            // from < to =>> to - from >= 0 ? no overflow!
            // from < length
            // to < length
            Objects.checkFromIndexSize(from, size, length); // length invariant
        }
    }

    @Test
    public static void testCheckFromToIndex(int start, int stop, int length, int offset, int size) {
        final int scale = 2;
        final int stride = 1;

        for (int i = start; i < stop; i += stride) {
            int from = scale * i + offset;
            int to = from + size;

            // from < to =>> to - from >= 0 ? no overflow!
            // from < length
            // to < length
            Objects.checkFromToIndex(from, to, length); // length invariant
        }
    }

    @Run(test = {
            "testCheckFromIndexSize",
//             "testUnintrinsifiedCheckFromToIndex",
            "testCheckFromToIndex"
    })
    private void testCheckFromToIndex_runner() {
//         testUnintrinsifiedCheckFromToIndex(0, 100, 210, 0, 10);
        testCheckFromIndexSize(0, 100, 210, 0, 10);
        testCheckFromToIndex(0, 100, 210, 0, 10);
    }
}