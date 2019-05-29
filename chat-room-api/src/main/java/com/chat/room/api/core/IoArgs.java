package com.chat.room.api.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class IoArgs {

    private byte[] byteBuffer = new byte[256];
    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    public int read(SocketChannel channel) throws IOException {
        buffer.clear();
        return channel.read(buffer);
    }


    public int write(SocketChannel channel) throws IOException {
        return channel.write(buffer);
    }

    public String bufferString() {
        return new String(byteBuffer, 0, buffer.position() - 1);
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
