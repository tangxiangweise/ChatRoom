package com.chat.room.api.core.impl.async;

import com.chat.room.api.box.abs.SendPacket;
import com.chat.room.api.core.IoArgs;
import com.chat.room.api.core.SendDispatcher;
import com.chat.room.api.core.Sender;
import com.chat.room.api.utils.CloseUtils;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher, IoArgs.IoArgsEventProcessor, AsyncPacketReader.PacketProvider {

    private final Sender sender;
    /**
     * 当前发送的packet的大小，以及进度
     */
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean();

    private final AsyncPacketReader reader = new AsyncPacketReader(this);

    private final Object queueLock = new Object();

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        sender.setSendListener(this);
    }

    @Override
    public void send(SendPacket packet) {
        synchronized (queueLock) {
            queue.offer(packet);
            if (isSending.compareAndSet(false, true)) {
                if (reader.requestTakePacket()) {
                    requestSend();
                }
            }
        }
    }

    @Override
    public void cancel(SendPacket packet) {
        boolean result;
        synchronized (queueLock) {
            result = queue.remove(packet);
        }
        if (result) {
            packet.cancel();
            return;
        }
        reader.cancel(packet);
    }

    @Override
    public SendPacket takePacket() {
        SendPacket packet;
        synchronized (queueLock) {
            packet = queue.poll();
            if (packet == null) {
                //队列为空，取消发送状态
                isSending.set(false);
                return null;
            }
        }
        if (packet.isCanceled()) {
            //已取消 不用发送
            packet = takePacket();
        }
        return packet;
    }

    /**
     * 完成packet发送
     *
     * @param isSucceed
     */
    @Override
    public void completedPacket(SendPacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
    }

    /**
     * 请求网络进行数据发送
     */
    private void requestSend() {
        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            isSending.set(false);
            //reader关闭
            reader.close();
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        return reader.fillData();
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        //继续发送当前包
        if (reader.requestTakePacket()) {
            requestSend();
        }
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        if (args != null) {
            e.printStackTrace();
        }
    }

}
