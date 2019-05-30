package com.chat.room.api.core;

import com.chat.room.api.box.ReceivePacket;
import com.chat.room.api.box.SendPacket;
import com.chat.room.api.box.impl.StringReceivePacket;
import com.chat.room.api.box.impl.StringSendPacket;
import com.chat.room.api.core.impl.SocketChannelAdapter;
import com.chat.room.api.core.impl.async.AsyncReceiveDispatcher;
import com.chat.room.api.core.impl.async.AsyncSendDispatcher;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

public class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {

    private String key = UUID.randomUUID().toString();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;
    private SendDispatcher sendDispatcher;
    private ReceiveDispatcher receiveDispatcher;

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

    }

    protected void onReceiveNewMessage(String msg) {
        System.out.println(key + " : " + msg);
    }


    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {
            if (packet instanceof StringReceivePacket) {
                String msg = ((StringReceivePacket) packet).string();
                onReceiveNewMessage(msg);
            }
        }
    };

}
