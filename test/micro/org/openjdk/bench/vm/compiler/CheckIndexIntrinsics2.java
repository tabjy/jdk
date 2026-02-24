package org.openjdk.bench.vm.compiler;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(2)
public class CheckIndexIntrinsics2 {
    static abstract class Buffer {
//        static Buffer buffer(int size) {
//            return new BufferImpl(size);
//        }

        abstract void setByte(int index, byte b);

        abstract byte getByte(int index);
    }

    static class BufferImplGoodVertex extends Buffer {
        ByteBuffer buffer;

        BufferImplGoodVertex(int size) {
            buffer = ByteBuffer.heapBuffer(size);
        }

        void setByte(int index, byte b) {
            // ensureLength
            buffer.setByte(index, b);
        }

        @Override
        byte getByte(int index) {
            checkUpperBound(index, 1);
            return buffer.getByte(index);
        }

        int checkFromIndexSize(int fromIndex, int size, int length) {
            if ((length | fromIndex | size) < 0 || size > length - fromIndex)
                throw new IndexOutOfBoundsException("obb");
            return fromIndex;
        }

        private void checkUpperBound(int index, int size) {
            int length = buffer.writerIndex();
            if (index < 0 || index + size < 0 || index + size > length) {
                throw new IndexOutOfBoundsException(index + " + " + size + " > " + length);
            }
        }
    }

    static class BufferImplBadVertex extends Buffer {
        ByteBuffer buffer;

        BufferImplBadVertex(int size) {
            buffer = ByteBuffer.heapBuffer(size);
        }

        void setByte(int index, byte b) {
            // ensureLength
            buffer.setByte(index, b);
        }

        @Override
        byte getByte(int index) {
            checkUpperBound(index, 1);
            return buffer.getByte(index);
        }

        int checkFromIndexSize(int fromIndex, int size, int length) {
            if ((length | fromIndex | size) < 0 || size > length - fromIndex)
                throw new IndexOutOfBoundsException("obb");
            return fromIndex;
        }

        private void checkUpperBound(int index, int size) {
            int length = buffer.writerIndex();

            if ((index | length - (index + size)) < 0) {
                throw new IndexOutOfBoundsException(index + " + " + size + " > " + length);
            }
        }
    }

    static class BufferImplBytecode extends Buffer {
        ByteBuffer buffer;

        BufferImplBytecode(int size) {
            buffer = ByteBuffer.heapBuffer(size);
        }

        void setByte(int index, byte b) {
            // ensureLength
            buffer.setByte(index, b);
        }

        @Override
        byte getByte(int index) {
            checkUpperBound(index, 1);
            return buffer.getByte(index);
        }

        int checkFromIndexSize(int fromIndex, int size, int length) {
            if ((length | fromIndex | size) < 0 || size > length - fromIndex)
                throw new IndexOutOfBoundsException("obb");
            return fromIndex;
        }

        private void checkUpperBound(int index, int size) {
            int length = buffer.writerIndex();

            checkFromIndexSize(index, size, length);
        }
    }

    static class BufferImplIntrinsified extends Buffer {
        ByteBuffer buffer;

        BufferImplIntrinsified(int size) {
            buffer = ByteBuffer.heapBuffer(size);
        }

        void setByte(int index, byte b) {
            // ensureLength
            buffer.setByte(index, b);
        }

        @Override
        byte getByte(int index) {
            checkUpperBound(index, 1);
            return buffer.getByte(index);
        }

        private void checkUpperBound(int index, int size) {
            int length = buffer.writerIndex();

            Objects.checkFromIndexSize(index, size, length);
        }
    }

    static class ByteBuffer {
        byte[] bytes;

        ByteBuffer(int size) {
            bytes = new byte[size];
        }

        static ByteBuffer heapBuffer(int size) {
            return new ByteBuffer(size);
        }

        public void setByte(int index, byte b) {
            this.bytes[index] = b;
        }

        public byte getByte(int index) {
            return 0;
//             return bytes[index];
        }

        int writerIndex() {
            return bytes.length;
        }
    }

    private Buffer goodVertxBuffer;
    private Buffer badVertxBuffer;
    private Buffer bytecodeBuffer;
    private Buffer intrinsifiedBuffer;
    @Param({"64"})
    private int batchSize;


    @Setup
    public void setup() {
//        vertxBuffer = Buffer.buffer(batchSize);
        goodVertxBuffer = new BufferImplGoodVertex(batchSize);
        badVertxBuffer = new BufferImplBadVertex(batchSize);
        bytecodeBuffer = new BufferImplBytecode(batchSize);
        intrinsifiedBuffer = new BufferImplIntrinsified(batchSize);
        for (int i = 0; i < batchSize; i++) {
//            vertxBuffer.setByte(i, (byte) i);
            goodVertxBuffer.setByte(i, (byte) i);
            badVertxBuffer.setByte(i, (byte) i);
            bytecodeBuffer.setByte(i, (byte) i);
            intrinsifiedBuffer.setByte(i, (byte) i);
        }
    }

    @Benchmark
    public void goodVertxBuffer(Blackhole bh) {
        Buffer buffer = this.goodVertxBuffer;
        for (int i = 0, size = batchSize; i < size; i++) {
            bh.consume(buffer.getByte(i)); // checkFromIndexSize(i, 1, size)
        }
    }

    @Benchmark
    public void badVertxBuffer(Blackhole bh) {
        Buffer buffer = this.badVertxBuffer;
        for (int i = 0, size = batchSize; i < size; i++) {
            bh.consume(buffer.getByte(i));
        }
    }

    @Benchmark
    public void bytecodeBuffer(Blackhole bh) {
        Buffer buffer = this.bytecodeBuffer;
        for (int i = 0, size = batchSize; i < size; i++) {
            bh.consume(buffer.getByte(i));
        }
    }

    @Benchmark
    public void intrinsifiedBuffer(Blackhole bh) {
        Buffer buffer = this.intrinsifiedBuffer;
        for (int i = 0, size = batchSize; i < size; i++) {
            bh.consume(buffer.getByte(i));
        }
    }
}
