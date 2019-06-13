package com.chat.room.api.core;

import com.chat.room.api.box.*;
import com.chat.room.api.box.abs.Packet;
import com.chat.room.api.box.abs.ReceivePacket;
import com.chat.room.api.box.abs.SendPacket;
import com.chat.room.api.core.impl.SocketChannelAdapter;
import com.chat.room.api.core.impl.async.AsyncReceiveDispatcher;
import com.chat.room.api.core.impl.async.AsyncSendDispatcher;
import com.chat.room.api.core.impl.bridge.BridgeSocketDispatcher;
import com.chat.room.api.core.schedule.ScheduleJob;
import com.chat.room.api.core.schedule.Scheduler;
import com.chat.room.api.utils.CloseUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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

    /**
     * 调度一份任务
     * @param job 任务
     */
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

    /**
     * 改变当前调度器为桥接模式
     */
    public void changeToBridge() {
        if (receiveDispatcher instanceof BridgeSocketDispatcher) {
            //已改变直接返回
            return;
        }
        //老的停止
        receiveDispatcher.stop();
        // 构建新的接收者调度器
        BridgeSocketDispatcher dispatcher = new BridgeSocketDispatcher(receiver);
        receiveDispatcher = dispatcher;
        //启动
        dispatcher.start();
    }

    /**
     * 将另外一个链接的发送者绑定到当前链接的桥接调度器上实现两个链接的桥接功能
     *
     * @param sender 另外一个链接的发送者
     */
    public void bindToBridge(Sender sender) {
        if (sender == this.sender) {
            throw new UnsupportedOperationException("Can not set current connector sender ");
        } else if (!(receiveDispatcher instanceof BridgeSocketDispatcher)) {
            throw new IllegalStateException("receiveDispatcher is not BridgeSocketDispatcher");
        }
        ((BridgeSocketDispatcher) receiveDispatcher).bindSender(sender);
    }

    /**
     * 将之前链接的发送者解除绑定，解除桥接数据发送功能
     */
    public void unBindToBridge() {
        if (!(receiveDispatcher instanceof BridgeSocketDispatcher)) {
            throw new IllegalStateException("receiveDispatcher is not BridgeSocketDispatcher");
        }
        ((BridgeSocketDispatcher) receiveDispatcher).bindSender(null);
    }

    /**
     * 获取当前链接的发送者
     * @return
     */
    public Sender getSender() {
        return sender;
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

    /**
     * 当接受包是文件时，需要得到一份空的文件用以数据存储
     *
     * @param length     长度
     * @param headerInfo 额外信息
     * @return 新的文件
     */
    protected abstract File createNewReceiveFile(long length, byte[] headerInfo);

    /**
     * 当接受包是直流数据包时，需要得到一个用以存储当前直流数据的输出流
     * 所有接收到的数据都将通过输出流输出
     *
     * @param length     长度
     * @param headerInfo 额外信息
     * @return 输出流
     */
    protected abstract OutputStream createNewReceiveDirectOutputStream(long length, byte[] headerInfo);

    /**
     * 当收到一个新的包packet时会进行回调的内部类
     */
    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {

        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {
            onReceivedPacket(packet);
        }

        @Override
        public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length, byte[] headerInfo) {
            switch (type) {
                case Packet.TYPE_MEMORY_BYTES:
                    return new BytesReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReceivePacket(length);
                case Packet.TYPE_STREAM_FILE:
                    return new FileReceivePacket(length, createNewReceiveFile(length, headerInfo));
                case Packet.TYPE_STREAM_DIRECT:
                    return new StreamDirectReceivePacket(createNewReceiveDirectOutputStream(length, headerInfo), length);
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
