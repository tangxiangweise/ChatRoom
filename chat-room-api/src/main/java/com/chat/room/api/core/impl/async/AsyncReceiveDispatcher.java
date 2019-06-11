package com.chat.room.api.core.impl.async;

import com.chat.room.api.box.abs.ReceivePacket;
import com.chat.room.api.core.IoArgs;
import com.chat.room.api.core.ReceiveDispatcher;
import com.chat.room.api.core.Receiver;
import com.chat.room.api.utils.CloseUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher, IoArgs.IoArgsEventProcessor, AsyncPacketWriter.PacketProvider {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Receiver receiver;
    private final ReceivePacketCallback callback;
    private final AsyncPacketWriter writer = new AsyncPacketWriter(this);

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback callback) {
        this.receiver = receiver;
        this.callback = callback;
        this.receiver.setReceiveListener(this);
    }

    @Override
    public void start() {
        registerReceive();
    }

    @Override
    public void stop() {

    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            writer.close();
        }
    }

    private void registerReceive() {
        try {
            receiver.postReceiveAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }


    @Override
    public IoArgs provideIoArgs() {
        IoArgs args = writer.takeIoArgs();
        //一份新的IoArgs 需要调用一次开始写入数据的操作
        args.startWriting();
        return args;
    }

    @Override
    public ReceivePacket takePacket(byte type, long length, byte[] headerInfo) {
        return callback.onArrivedNewPacket(type, length);
    }

    @Override
    public void completedPacket(ReceivePacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
        callback.onReceivePacketCompleted(packet);
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        if (isClosed.get()) {
            return;
        }
        //消费数据之前标示args数据填充完成
        //改变未可读取数据状态
        args.finishWriting();
        //有数据则重复消费
        do {
            writer.consumeIoArgs(args);
        } while (args.remained() && !isClosed.get());
        //继续接收下一条数据
        registerReceive();
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onReceivedHeartbeat() {
        callback.onReceiveHeartbeat();
    }
}
