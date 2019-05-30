package com.chat.room.api.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class IoArgs {

    private int limit = 256;
    private byte[] byteBuffer = new byte[256];
    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    /**
     * 从bytes中读取数据
     *
     * @param bytes
     * @param offset
     * @return
     */
    public int readFrom(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.put(bytes, offset, size);
        return size;
    }

    /**
     * 写入数据到bytes中
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
     * 从SocketChannel中读取数据
     */
    public int readFrom(SocketChannel channel) throws IOException {
        startWriting();
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }
        finishWriting();
        return bytesProduced;
    }

    /**
     * 写入数据到SocketChannel中
     */
    public int writeTo(SocketChannel channel) throws IOException {
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
     * @param limit
     */
    public void limit(int limit) {
        this.limit = limit;
    }

    public void writeLength(int total) {
        buffer.putInt(total);
    }

    public int readLength() {
        return buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }


    public interface IoArgsEventListener {

        void onStarted(IoArgs args);

        /**
         * 打印数据并读取下一条
         *
         * @param args
         */
        void onCompleted(IoArgs args);

    }
}
