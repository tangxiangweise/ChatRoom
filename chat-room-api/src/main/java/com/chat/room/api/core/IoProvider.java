package com.chat.room.api.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

public interface IoProvider extends Closeable {


    boolean registerInput(SocketChannel channel, HandleProviderCallback callback);

    boolean registerOutput(SocketChannel channel, HandleProviderCallback callback);

    void unRegisterOutput(SocketChannel channel);

    void unRegisterInput(SocketChannel channel);


    /**
     * 发送数据类
     */
    abstract class HandleProviderCallback implements Runnable {

        protected volatile IoArgs attach;

        @Override
        public void run() {
            onProviderIo(attach);
        }

        public void checkAttachNull() {
            if (attach != null) {
                throw new IllegalStateException("attach is not null ");
            }
        }

        protected abstract void onProviderIo(IoArgs args);
    }

}
