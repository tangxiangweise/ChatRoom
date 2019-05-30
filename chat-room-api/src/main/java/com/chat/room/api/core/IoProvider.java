package com.chat.room.api.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

public interface IoProvider extends Closeable {


    boolean registerInput(SocketChannel channel, HandleInputCallback callback);

    boolean registerOutput(SocketChannel channel, HandleOutputCallback callback);

    void unRegisterOutput(SocketChannel channel);

    void unRegisterInput(SocketChannel channel);


    /**
     * 发送数据类
     */
    abstract class HandleOutputCallback implements Runnable {
        /**
         * 需要发送的数据
         */
        private Object attach;

        @Override
        public void run() {
            canProviderOutput(attach);
        }

        public final void setAttach(Object attach) {
            this.attach = attach;
        }

        public final <T> T getAttach() {
            return (T) attach;
        }

        protected abstract void canProviderOutput(Object attach);
    }

    /**
     * 接收数据类
     */
    abstract class HandleInputCallback implements Runnable {
        @Override
        public void run() {
            canProviderInput();
        }

        protected abstract void canProviderInput();
    }
}