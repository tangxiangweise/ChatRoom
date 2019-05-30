package com.chat.room.api.core.impl.async;

import com.chat.room.api.box.SendPacket;
import com.chat.room.api.core.IoArgs;
import com.chat.room.api.core.SendDispatcher;
import com.chat.room.api.core.Sender;
import com.chat.room.api.utils.CloseUtils;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher {

    private int total;
    private int position;
    private final Sender sender;
    private SendPacket packetTemp;
    private IoArgs ioArgs = new IoArgs();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean();

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
    }

    @Override
    public void send(SendPacket packet) {
        queue.offer(packet);
        if (isSending.compareAndSet(false, true)) {
            sendNextPacket();
        }
    }

    @Override
    public void cancel(SendPacket packet) {

    }

    private SendPacket takePacket() {
        SendPacket packet = queue.poll();
        if (packet != null && packet.isCanceled()) {
            //已取消 不用发送
            packet = takePacket();
        }
        return packet;
    }

    private void sendNextPacket() {
        SendPacket temp = packetTemp;
        if (temp != null) {
            CloseUtils.close(temp);
        }
        SendPacket packet = packetTemp = takePacket();
        if (packet == null) {
            //队列为空，取消发送数据
            isSending.set(false);
            return;
        }
        total = packet.length();
        position = 0;
        sendCurrentPacket();
    }

    private void sendCurrentPacket() {
        IoArgs args = this.ioArgs;
        args.startWriting();
        if (position >= total) {
            sendNextPacket();
            return;
        } else if (position == 0) {
            //首包
            args.writeLength(total);
        }
        byte[] bytes = packetTemp.bytes();
        //把bytes的数据写入到IoArgs
        int count = args.readFrom(bytes, position);
        position += count;
        args.finishWriting();
        try {
            sender.sendAsync(args, ioArgsEventListener);
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            isSending.set(false);
            SendPacket packet = this.packetTemp;
            if (packet != null) {
                packetTemp = null;
                CloseUtils.close(packet);
            }
        }
    }

    private final IoArgs.IoArgsEventListener ioArgsEventListener = new IoArgs.IoArgsEventListener() {
        @Override
        public void onStarted(IoArgs args) {

        }

        @Override
        public void onCompleted(IoArgs args) {
            //继续发送当前包
            sendCurrentPacket();

        }
    };

}
