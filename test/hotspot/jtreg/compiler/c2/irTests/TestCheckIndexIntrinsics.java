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
import java.util.Random;
import java.util.function.Supplier;

import jdk.test.lib.Utils;

/*
 * @test
 * @bug 8361837
 * @summary C2: investigate intrinsification of Preconditions.checkFromToIndex() and Preconditions.checkFromIndexSize()
 * @library /test/lib /
 * @requires vm.compiler2.enabled
 * @run driver compiler.c2.irTests.TestCheckIndexIntrinsics
 */
public class TestCheckIndexIntrinsics {
    private static final Random RNG = Utils.getRandomInstance();

    public static void main(String[] args) {
        // TODO: remove PrintIdeal flag
        TestFramework.runWithFlags("-XX:PrintIdealGraphLevel=2", "-XX:CompileOnly=compiler.c2.irTests.TestCheckIndexIntrinsics::*", "-XX:LoopMaxUnroll=0");

        testCorrectness();
    }

    // Unintrinsified bytecode functions, as in jdk.internal.util.Preconditions
    private static int unintrinsifiedCheckIndex(int index, int length) {
        if (index < 0 || index >= length)
            throw new IndexOutOfBoundsException("oob");
        return index;
    }

    private static int unintrinsifiedCheckFromToIndex(int fromIndex, int toIndex, int length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length)
            throw new IndexOutOfBoundsException("oob");
        return fromIndex;
    }

    private static int unintrinsifiedCheckFromIndexSize(int fromIndex, int size, int length) {
        if ((length | fromIndex | size) < 0 || size > length - fromIndex)
            throw new IndexOutOfBoundsException("oob");
        return fromIndex;
    }

    // Corresponding long versions
    private static long unintrinsifiedCheckIndexL(long index, long length) {
        if (index < 0 || index >= length)
            throw new IndexOutOfBoundsException("oob");
        return index;
    }

    private static long unintrinsifiedCheckFromToIndexL(long fromIndex, long toIndex, long length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length)
            throw new IndexOutOfBoundsException("oob");
        return fromIndex;
    }

    private static long unintrinsifiedCheckFromIndexSizeL(long fromIndex, long size, long length) {
        if ((length | fromIndex | size) < 0 || size > length - fromIndex)
            throw new IndexOutOfBoundsException("oob");
        return fromIndex;
    }

    // TODO: re-enable tests
   // Controlled test without intrinsics, should not have range checks (and traps) to begin with.
   @Test
   @IR(counts = { IRNode.COUNTED_LOOP, ">= 1"})
   @IR(failOn = { IRNode.RANGE_CHECK_TRAP })
   public static void testUnintrinsifiedCheckIndex(int start, int stop, int length, int offset) {
       final int scale = 2;
       final int stride = 1;

       for (int i = start; i < stop; i += stride) {
           unintrinsifiedCheckIndex(scale * i + offset, length);
       }
   }

   // Same but for longs
   @Test
   @IR(counts = { IRNode.COUNTED_LOOP, ">= 1"})
   @IR(failOn = { IRNode.RANGE_CHECK_TRAP })
   public static void testUnintrinsifiedCheckIndexL(long start, long stop, long length, long offset) {
       final long scale = 2;
       final long stride = 1;

       for (long i = start; i < stop; i += stride) {
           unintrinsifiedCheckIndexL(scale * i + offset, length);
       }
   }

   // Test range check (and trap) successfully eliminated
   @Test
   @IR(failOn = { IRNode.COUNTED_LOOP })
   @IR(counts = { IRNode.RANGE_CHECK_TRAP, "1" }, phase = CompilePhase.AFTER_PARSING)
   @IR(failOn = { IRNode.RANGE_CHECK_TRAP }) // phase = CompilePhase.BEFORE_MATCHING
   public static void testCheckIndex(int start, int stop, int length, int offset) {
       final int scale = 2;
       final int stride = 1;

       for (int i = start; i < stop; i += stride) {
           Objects.checkIndex(scale * i + offset, length);
       }
   }

   // Same but for longs
   @Test
   @IR(failOn = { IRNode.COUNTED_LOOP })
   @IR(counts = { IRNode.RANGE_CHECK_TRAP, "1" }, phase = CompilePhase.AFTER_PARSING)
   @IR(failOn = { IRNode.RANGE_CHECK_TRAP }) // phase = CompilePhase.BEFORE_MATCHING
   public static void testCheckIndexL(long start, long stop, long length, long offset) {
       final long scale = 2;
       final long stride = 1;

       for (long i = start; i < stop; i += stride) {
           Objects.checkIndex(scale * i + offset, length);
       }
   }

   @Run(test = {
           "testUnintrinsifiedCheckIndex",
           "testUnintrinsifiedCheckIndexL",
           "testCheckIndex",
           "testCheckIndexL"
   })
   private void testCheckIndex_runner() {
       testUnintrinsifiedCheckIndex(0, 100, 200, 0);
       testUnintrinsifiedCheckIndexL(0, 100, 200, 0);
       testCheckIndex(0, 100, 200, 0);
       testCheckIndexL(0, 100, 200, 0);
   }

    @Test
    @IR(counts = { IRNode.COUNTED_LOOP, "3"}) // pre/main/post loops
    @IR(failOn = { IRNode.RANGE_CHECK_TRAP })
    public static void testUnintrinsifiedCheckFromToIndex(int start, int stop, int length, int offset, int size) {
        final int scale = 2;
        final int stride = 1;

        for (int i = start; i < stop; i += stride) {
            int from = scale * i + offset;
            int to = from + size;

            unintrinsifiedCheckFromToIndex(from, to, length);
        }
    }

    @Test
    @IR(counts = { IRNode.COUNTED_LOOP, "2"}) // range check in main loop hoisted and main loop is eliminated
    @IR(counts = { IRNode.RANGE_CHECK_TRAP, "3" }, phase = CompilePhase.AFTER_PARSING)
    @IR(counts = { IRNode.RANGE_CHECK_TRAP, "2" })
    public static void testCheckFromToIndex(int start, int stop, int length, int offset, int size) {
        final int scale = 2;
        final int stride = 1;

        for (int i = start; i < stop; i += stride) {
            int from = scale * i + offset;
            int to = from + size;

            // from < to =>> to - from >= 0 ? no overflow!
            // from < length
            // to < length
            Objects.checkFromToIndex(from, to, length); // to - from >= 0, from + size - from = size ?>= 0
        }
    }

    @Run(test = {
            "testUnintrinsifiedCheckFromToIndex",
            "testCheckFromToIndex"
    })
    private void testCheckFromToIndex_runner() {
        testUnintrinsifiedCheckFromToIndex(0, 100, 210, 0, 10);
        testCheckFromToIndex(0, 100, 210, 0, 10);
    }

    @Test
    @IR(counts = { IRNode.COUNTED_LOOP, "3"}) // pre/main/post loops
    @IR(failOn = { IRNode.RANGE_CHECK_TRAP })
    public static void testUnintrinsifiedFromIndexSize(int start, int stop, int length, int offset, int size) {
        final int scale = 2;
        final int stride = 1;

        for (int i = start; i < stop; i += stride) {
            int from = scale * i + offset;

            // from < to =>> to - from >= 0 ? no overflow!
            // from < length
            // to < length
            unintrinsifiedCheckFromIndexSize(from, size, length);
        }
    }

    @Test
    @IR(counts = { IRNode.COUNTED_LOOP, "2"}) // range check in pre/post loop, main loop becomes empty and eliminated
    @IR(counts = { IRNode.RANGE_CHECK_TRAP, "2" })
    public static void testCheckFromIndexSize(int start, int stop, int length, int offset, int size) {
        final int scale = 2;
        final int stride = 1;

        for (int i = start; i < stop; i += stride) {
            int from = scale * i + offset;

            // from < to =>> to - from >= 0 ? no overflow!
            // from < length
            // to < length
            Objects.checkFromIndexSize(from, size, length);
        }
    }

    @Run(test = {
                "testUnintrinsifiedFromIndexSize",
                "testCheckFromIndexSize",
    })
    private void testCheckFromIndexSize_runner() {
        testUnintrinsifiedFromIndexSize(0, 100, 210, 0, 10);
        testCheckFromIndexSize(0, 100, 210, 0, 10);
    }

    private static void assertEqual(Supplier<Number> groundTruth, Supplier<Number> intrinsified) {
        boolean oob = false;
        Number expected = null;
        try {
            expected = groundTruth.get();
        } catch (IndexOutOfBoundsException e) {
            oob = true;
        }

        // TODO: does jvm fall back to interpreter mode for the subsequent call after an exception?
        if (oob) {
            return;
        }

        try {
            Number observed = intrinsified.get();
            if (!expected.equals(observed)) throw new RuntimeException("should be equal!");
        } catch (IndexOutOfBoundsException e) {
            if (!oob) throw new RuntimeException("should raise exception!");
        }
    }

    private static void testCorrectness() {
        // warm up
        // TODO: do I even need to warm up intrinsified methods?
        for (int i = 0; i < 20_000; i++) {
            Objects.checkIndex(0, 42);
            Objects.checkFromToIndex(1, 16, 42);
            Objects.checkFromIndexSize(32, 42, 123);
        }

        int[] values = {
            -1, -2, -10, -100, -1024, -10000, -999999,
            0, 1, 2, 42, 64, 100, 123, 1024, 0xC0FFEE,
            Integer.MAX_VALUE - 1, Integer.MAX_VALUE, Integer.MIN_VALUE + 1, Integer.MIN_VALUE,
            RNG.nextInt(), RNG.nextInt(), RNG.nextInt(), RNG.nextInt()
        };

        for (int i : values) {
            for (int j : values) {
                for (int k : values) {
                    assertEqual(() -> unintrinsifiedCheckIndex(i, j), () -> Objects.checkIndex(i, j));
                    assertEqual(() -> unintrinsifiedCheckFromToIndex(i, j, k), () -> Objects.checkFromToIndex(i, j, k));
                    assertEqual(() -> unintrinsifiedCheckFromIndexSize(i, j, k), () -> Objects.checkFromIndexSize(i, j, k));
                }
            }
        }
    }
}
