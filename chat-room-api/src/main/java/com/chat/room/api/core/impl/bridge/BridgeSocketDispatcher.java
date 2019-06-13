package com.chat.room.api.core.impl.bridge;

import com.chat.room.api.box.abs.SendPacket;
import com.chat.room.api.core.*;
import com.chat.room.api.utils.plugin.CircularByteBuffer;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 桥接调度器实现
 * 当前调度器同时实现来了发送者与接受者调度逻辑
 * 核心思想：把接受者接收到的数据全部转发给发送者
 */
public class BridgeSocketDispatcher implements ReceiveDispatcher, SendDispatcher {

    // 数据暂存的缓冲区
    private final CircularByteBuffer byteBuffer = new CircularByteBuffer(512, true);
    // 根据缓冲区得到的读取、写入通道
    private final ReadableByteChannel readableByteChannel = Channels.newChannel(byteBuffer.getInputStream());
    private final WritableByteChannel writableByteChannel = Channels.newChannel(byteBuffer.getOutputStream());
    // 有数据则接受，无数据不强求填满、有多少返回多少
    private final IoArgs recieveIoArgs = new IoArgs(256, false);
    private final Receiver receiver;
    // 当前是否处于发送中
    private final AtomicBoolean isSending = new AtomicBoolean();
    // 用以发送的IoArgs，默认全部发送数据
    private final IoArgs sendIoArgs = new IoArgs();
    private volatile Sender sender;

    public BridgeSocketDispatcher(Receiver receiver) {
        this.receiver = receiver;
    }

    /**
     * 绑定一个新的发送者，在绑定时，将老的发送者对应的调度设置为null
     *
     * @param sender 新的发送者
     */
    public void bindSender(Sender sender) {
        //清理老的发送者回调
        final Sender oldSender = this.sender;
        if (oldSender != null) {
            oldSender.setSendListener(null);
        }
        //清理操作
        synchronized (isSending) {
            isSending.set(false);
        }
        byteBuffer.clear();
        //设置新的发送者
        this.sender = sender;
        if (sender != null) {
            sender.setSendListener(senderEventProcessor);
            requestSend();
        }
    }

    /**
     * 外部初始化好了桥接调度器后需要调用start方法开始
     */
    @Override
    public void start() {
        receiver.setReceiveListener(receiverEventProcessor);
        registerReceive();
    }


    @Override
    public void stop() {

    }

    @Override
    public void send(SendPacket packet) {

    }

    @Override
    public void sendHeartbeat() {

    }

    @Override
    public void cancel(SendPacket packet) {

    }

    @Override
    public void close() throws IOException {

    }

    /**
     * 请求网络进行数据发送
     */
    private void requestSend() {
        synchronized (isSending) {
            final Sender sender = this.sender;
            if (isSending.get() || sender == null) {
                return;
            }
            //返回true 代表当前有数据需要发送
            if (byteBuffer.getAvailable() > 0) {
                try {
                    boolean isSucceed = sender.postSendAsync();
                    if (isSucceed) {
                        isSending.set(true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 请求网络进行数据接收
     */
    private void registerReceive() {
        try {
            receiver.postReceiveAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收者回调
     */
    private final IoArgs.IoArgsEventProcessor receiverEventProcessor = new IoArgs.IoArgsEventProcessor() {
        @Override
        public IoArgs provideIoArgs() {
            recieveIoArgs.resetLimit();
            //一份新的IoArgs需要调用一次开始写入数据的操作
            recieveIoArgs.startWriting();
            return recieveIoArgs;
        }

        @Override
        public void onConsumeCompleted(IoArgs args) {
            args.finishWriting();
            try {
                args.writeTo(writableByteChannel);
            } catch (IOException e) {
                e.printStackTrace();
            }
            registerReceive();
            //接受数据后请求发送数据
            requestSend();
        }

        @Override
        public void onConsumeFailed(IoArgs args, Exception e) {
            e.printStackTrace();
        }
    };

    /**
     * 发送者回调
     */
    private final IoArgs.IoArgsEventProcessor senderEventProcessor = new IoArgs.IoArgsEventProcessor() {
        @Override
        public IoArgs provideIoArgs() {
            try {
                int available = byteBuffer.getAvailable();
                IoArgs args = BridgeSocketDispatcher.this.sendIoArgs;
                if (available > 0) {
                    args.limit(available);
                    args.startWriting();
                    args.readFrom(readableByteChannel);
                    args.finishWriting();
                    return args;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void onConsumeCompleted(IoArgs args) {
            //设置当前发送状态
            synchronized (isSending) {
                isSending.set(false);
            }
            //继续请求发送当前的数据
            requestSend();
        }

        @Override
        public void onConsumeFailed(IoArgs args, Exception e) {
            e.printStackTrace();
            //设置当前发送状态
            synchronized (isSending) {
                isSending.set(false);
            }
            //继续请求发送当前的数据
            requestSend();
        }
    };

}
