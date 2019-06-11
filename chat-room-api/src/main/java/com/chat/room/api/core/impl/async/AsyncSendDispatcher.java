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
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final AsyncPacketReader reader = new AsyncPacketReader(this);

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        sender.setSendListener(this);
    }

    /**
     * 发送数据
     *
     * @param packet
     */
    @Override
    public void send(SendPacket packet) {
        queue.offer(packet);
        requestSend();
    }

    /**
     * 发送心跳包
     */
    @Override
    public void sendHeartbeat() {
        if (queue.size() > 0) {
            return;
        }
        if (reader.requestSendHeartbeatFrame()) {
            requestSend();
        }
    }

    /**
     * 取消发送packet
     *
     * @param packet
     */
    @Override
    public void cancel(SendPacket packet) {
        boolean result = queue.remove(packet);
        if (result) {
            packet.cancel();
            return;
        }
        reader.cancel(packet);
    }

    /**
     * 从队列拿 packet
     *
     * @return
     */
    @Override
    public SendPacket takePacket() {
        SendPacket packet = queue.poll();
        if (packet == null) {
            return null;
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
        synchronized (isSending) {
            if (isSending.get() || isClosed.get()) {
                return;
            }
            //返回true代表当前有数据需要发送
            if (reader.requestTakePacket()) {
                try {
                    boolean isSucceed = sender.postSendAsync();
                    if (isSucceed) {
                        isSending.set(true);
                    }
                } catch (IOException e) {
                    closeAndNotify();
                }
            }
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            //reader关闭
            reader.close();
            queue.clear();
            synchronized (isSending) {
                isSending.set(false);
            }
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        return isClosed.get() ? null : reader.fillData();
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        synchronized (isSending) {
            isSending.set(false);
        }
        // 继续请求发送当前的数据
        requestSend();
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
        synchronized (isSending) {
            isSending.set(false);
        }
        // 继续请求发送当前的数据
        requestSend();
    }

}
