package com.chat.room.api.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

public class IoArgs {

    //单次操作最大区间
    private volatile int limit;
    // 是否需要消费所有区间（读取、写入）
    private final boolean isNeedConsumeRemaining;

    private final ByteBuffer buffer;

    public IoArgs(int size, boolean isNeedConsumeRemaining) {
        this.limit = size;
        this.isNeedConsumeRemaining = isNeedConsumeRemaining;
        this.buffer = ByteBuffer.allocate(size);
    }

    public IoArgs(int size) {
        this(size, true);
    }

    public IoArgs() {
        this(256);
    }

    /**
     * 从bytes数组进行消费
     *
     * @param bytes
     * @param offset
     * @param count
     * @return
     */
    public int readFrom(byte[] bytes, int offset, int count) {
        int size = Math.min(count, buffer.remaining());
        if (size <= 0) {
            return 0;
        }
        buffer.put(bytes, offset, size);
        return size;
    }

    /**
     * 写入数据到bytes
     *
     * @param bytes
     * @param offset
     * @return
     */
    public int writeTo(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset, size);
        return size;
    }

    /**
     * 从channel中读取数据
     *
     * @return
     */
    public int readFrom(ReadableByteChannel channel) throws IOException {
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }
        return bytesProduced;
    }

    /**
     * 写入数据到channel中
     *
     * @return
     */
    public int writeTo(WritableByteChannel channel) throws IOException {
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.write(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }
        return bytesProduced;
    }

    /**
     * 从SocketChannel中读取数据
     */
    public int readFrom(SocketChannel channel) throws IOException {
        ByteBuffer buffer = this.buffer;
        int bytesProduced = 0;
        int len;
        do {
            len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException("Cannot read any data with : " + channel);
            }
            bytesProduced += len;
        } while (buffer.hasRemaining() && len != 0);
        return bytesProduced;
    }

    /**
     * 写入数据到SocketChannel中
     */
    public int writeTo(SocketChannel channel) throws IOException {
        ByteBuffer buffer = this.buffer;
        int bytesProduced = 0;
        int len;
        do {
            len = channel.write(buffer);
            if (len < 0) {
                throw new EOFException("Current write any data with : " + channel);
            }
            bytesProduced += len;
        } while (buffer.hasRemaining() && len != 0);
        return bytesProduced;
    }


    /**
     * 开始写入数据到IoArgs
     */
    public void startWriting() {
        buffer.clear();
        buffer.limit(limit);
    }

    /**
     * 写完数据到调用
     */
    public void finishWriting() {
        buffer.flip();
    }

    /**
     * 设置单次写操作的容纳区间
     *
     * @param limit 区间大小
     */
    public void limit(int limit) {
        this.limit = Math.min(limit, buffer.capacity());
    }

    /**
     * 重置最大限制
     */
    public void resetLimit() {
        this.limit = buffer.capacity();
    }

    /**
     * 获取当前的容量
     *
     * @return 容量
     */
    public int capacity() {
        return buffer.capacity();
    }

    /**
     * 是否还有数据需要消费，或者说是否还有空闲区间需要容纳内容
     *
     * @return 还有数据存储或未消费区间
     */
    public boolean remained() {
        return buffer.remaining() > 0;
    }

    /**
     * 是否需要填满或完全消费所有数据
     *
     * @return 是否
     */
    public boolean isNeedConsumeRemaining() {
        return isNeedConsumeRemaining;
    }

    /**
     * 填充数据
     *
     * @param size 想要填充数据的长度
     * @return 真实填充数据的长度
     */
    public int fillEmpty(int size) {
        int fillSize = Math.min(size, buffer.remaining());
        buffer.position(buffer.position() + fillSize);
        return fillSize;
    }

    /**
     * 清空部分数据
     *
     * @param size 想要清空的数据长度
     * @return 真实清空的数据长度
     */
    public int setEmpty(int size) {
        int emptySize = Math.min(size, buffer.remaining());
        buffer.position(buffer.position() + emptySize);
        return emptySize;
    }


    /**
     * IoArgs 提供者、处理者；数据的生产或消费者
     */
    public interface IoArgsEventProcessor {
        /**
         * 提供一份可消费的IoArgs
         *
         * @return
         */
        IoArgs provideIoArgs();

        /**
         * 消费成功
         *
         * @param args
         */
        void onConsumeCompleted(IoArgs args);

        /**
         * 消费失败时回调
         *
         * @param args
         * @param e
         */
        void onConsumeFailed(IoArgs args, Exception e);

    }

}
