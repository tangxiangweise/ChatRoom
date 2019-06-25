package com.chat.room.api.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

public interface IoProvider extends Closeable {

    /**
     * 注册read事件
     * @param channel 通道
     * @param callback 读回调事件
     * @return
     */
    boolean registerInput(SocketChannel channel, HandleProviderCallback callback);
    /**
     * 注册write事件
     * @param channel 通道
     * @param callback 写回调事件
     * @return
     */
    boolean registerOutput(SocketChannel channel, HandleProviderCallback callback);

    /**
     * 解除事件
     * @param channel
     */
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
