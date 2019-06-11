package com.chat.room.api.core;

import com.chat.room.api.box.BytesReceivePacket;
import com.chat.room.api.box.FileReceivePacket;
import com.chat.room.api.box.StringReceivePacket;
import com.chat.room.api.box.StringSendPacket;
import com.chat.room.api.box.abs.Packet;
import com.chat.room.api.box.abs.ReceivePacket;
import com.chat.room.api.box.abs.SendPacket;
import com.chat.room.api.core.impl.SocketChannelAdapter;
import com.chat.room.api.core.impl.async.AsyncReceiveDispatcher;
import com.chat.room.api.core.impl.async.AsyncSendDispatcher;
import com.chat.room.api.core.schedule.ScheduleJob;
import com.chat.room.api.core.schedule.Scheduler;
import com.chat.room.api.utils.CloseUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {

    private String key = UUID.randomUUID().toString();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;
    private SendDispatcher sendDispatcher;
    private ReceiveDispatcher receiveDispatcher;
    private final List<ScheduleJob> scheduleJobs = new ArrayList<>(4);

    public void setup(SocketChannel socketChannel) throws IOException {
        this.channel = socketChannel;
        IoContext context = IoContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel, context.getIoProvider(), this);
        this.sender = adapter;
        this.receiver = adapter;

        sendDispatcher = new AsyncSendDispatcher(sender);
        receiveDispatcher = new AsyncReceiveDispatcher(receiver, receivePacketCallback);
        //接收启动
        receiveDispatcher.start();
    }

    public void send(String msg) {
        SendPacket packet = new StringSendPacket(msg);
        sendDispatcher.send(packet);
    }

    public void send(SendPacket packet) {
        sendDispatcher.send(packet);
    }


    public String getKey() {
        return key;
    }

    @Override
    public void close() throws IOException {
        receiveDispatcher.close();
        sendDispatcher.close();
        sender.close();
        receiver.close();
        channel.close();
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        synchronized (scheduleJobs) {
            for (ScheduleJob scheduleJob : scheduleJobs) {
                scheduleJob.unSchedule();
            }
            scheduleJobs.clear();
        }
        CloseUtils.close(this);
    }

    protected void onReceivedPacket(ReceivePacket packet) {
        System.out.println(key + " :[ New Packet ]- Type : " + packet.type() + " , Length : " + packet.length());
    }

    public void schedule(ScheduleJob job) {
        synchronized (scheduleJobs) {
            if (scheduleJobs.contains(job)) {
                return;
            }
            Scheduler scheduler = IoContext.get().getScheduler();
            job.schedule(scheduler);
            scheduleJobs.add(job);
        }
    }

    public long getLastActiveTime() {
        return Math.max(sender.getLastWriteTime(), receiver.getLastReadTime());
    }

    /**
     * 发送心跳包
     */
    public void fireIdleTimeoutEvent() {
        sendDispatcher.sendHeartbeat();
    }

    public void fireExceptionCaught(Throwable throwable) {

    }

    protected abstract File createNewReceiveFile();

    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {
            onReceivedPacket(packet);
        }

        @Override
        public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length) {
            switch (type) {
                case Packet.TYPE_MEMORY_BYTES:
                    return new BytesReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReceivePacket(length);
                case Packet.TYPE_STREAM_FILE:
                    return new FileReceivePacket(length, createNewReceiveFile());
                case Packet.TYPE_STREAM_DIRECT:
                    return new FileReceivePacket(length, createNewReceiveFile());
                default:
                    throw new UnsupportedOperationException("Unsupported packet type : " + type);
            }
        }

        @Override
        public void onReceiveHeartbeat() {
            System.out.println(getKey() + ": [Heartbeat]");
        }

    };


}
