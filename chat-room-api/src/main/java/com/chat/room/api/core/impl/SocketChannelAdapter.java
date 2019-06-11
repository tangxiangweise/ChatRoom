package com.chat.room.api.core.impl;

import com.chat.room.api.core.IoArgs;
import com.chat.room.api.core.IoProvider;
import com.chat.room.api.core.Receiver;
import com.chat.room.api.core.Sender;
import com.chat.room.api.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketChannelAdapter implements Sender, Receiver, Cloneable {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final SocketChannel channel;
    private final IoProvider ioProvider;
    private final OnChannelStatusChangedListener listener;

    private IoArgs.IoArgsEventProcessor receiveIoEventProcessor;

    private IoArgs.IoArgsEventProcessor sendIoEventProcessor;

    private volatile long lastReadTime = System.currentTimeMillis();

    private volatile long lastWriteTime = System.currentTimeMillis();

    private final IoProvider.HandleProviderCallback inputCallback = new IoProvider.HandleProviderCallback() {
        @Override
        protected void onProviderIo(IoArgs args) {
            if (isClosed.get()) {
                return;
            }
            lastReadTime = System.currentTimeMillis();
            IoArgs.IoArgsEventProcessor processor = receiveIoEventProcessor;
            if (args == null) {
                //拿到一份新的IoArgs
                args = processor.provideIoArgs();
            }
            try {
                if (args == null) {
                    processor.onConsumeFailed(null, new IOException("ProvideIoArgs is null"));
                } else {
                    int count = args.readFrom(channel);
                    if (count == 0) {
                        System.out.println("Current read zero data!");
                    }
                    if (args.remained()) {
                        //附加当前为消费完成的IoArgs
                        attach = args;
                        //再次注册数据发送
                        ioProvider.registerInput(channel, this);
                    } else {
                        //设置为null
                        attach = null;
                        //读取完成回调
                        processor.onConsumeCompleted(args);
                    }
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    private final IoProvider.HandleProviderCallback outputCallback = new IoProvider.HandleProviderCallback() {
        @Override
        protected void onProviderIo(IoArgs args) {
            if (isClosed.get()) {
                return;
            }
            lastWriteTime = System.currentTimeMillis();
            IoArgs.IoArgsEventProcessor processor = sendIoEventProcessor;
            if (args == null) {
                //拿到一份新的IoArgs
                args = processor.provideIoArgs();
            }
            try {
                if (args == null) {
                    processor.onConsumeFailed(null, new IOException("ProvideIoArgs is null"));
                } else {
                    int count = args.writeTo(channel);
                    if (count == 0) {
                        System.out.println("Current write zero data!");
                    }
                    if (args.remained()) {
                        //附加当前为消费完成的IoArgs
                        attach = args;
                        //再次注册数据发送
                        ioProvider.registerOutput(channel, this);
                    } else {
                        //设置为null
                        attach = null;
                        //输出完成回调
                        processor.onConsumeCompleted(args);
                    }
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider, OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);
    }

    @Override
    public boolean postReceiveAsync() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed !");
        }
        inputCallback.checkAttachNull();
        return ioProvider.registerInput(channel, inputCallback);
    }

    @Override
    public boolean postSendAsync() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed !");
        }
        outputCallback.checkAttachNull();
        return ioProvider.registerOutput(channel, outputCallback);
    }

    @Override
    public void setReceiveListener(IoArgs.IoArgsEventProcessor processor) {
        this.receiveIoEventProcessor = processor;
    }

    @Override
    public void setSendListener(IoArgs.IoArgsEventProcessor processor) {
        this.sendIoEventProcessor = processor;
    }


    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            //解除注册回调
            ioProvider.unRegisterInput(channel);
            ioProvider.unRegisterOutput(channel);
            //关闭
            CloseUtils.close(channel);
            //回调当前channel已关闭
            listener.onChannelClosed(channel);
        }
    }

    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }

    @Override
    public long getLastReadTime() {
        return lastReadTime;
    }

    @Override
    public long getLastWriteTime() {
        return lastWriteTime;
    }

}
