package com.chat.room.api.core.impl.stealing;

import com.chat.room.api.core.IoProvider;

import java.nio.channels.SocketChannel;

/**
 * 可用以进行任务调度的任务封装
 * 任务执行回调、当前任务类型、任务对应的通道
 */
public class IoTask {

    public final SocketChannel channel;
    public final IoProvider.HandleProviderCallback providerCallback;
    public final int ops;

    public IoTask(SocketChannel channel, int ops, IoProvider.HandleProviderCallback providerCallback) {
        this.channel = channel;
        this.providerCallback = providerCallback;
        this.ops = ops;
    }

}
