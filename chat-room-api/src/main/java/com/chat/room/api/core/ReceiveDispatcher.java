package com.chat.room.api.core;

import com.chat.room.api.box.abs.ReceivePacket;

import java.io.Closeable;

/**
 * 接收的数据调度封装
 * 把一份或者多个IoArgs组合成一份packet
 */
public interface ReceiveDispatcher extends Closeable {

    void start();

    void stop();

    interface ReceivePacketCallback {

        ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length, byte[] headerInfo);

        void onReceivePacketCompleted(ReceivePacket packet);

        void onReceiveHeartbeat();
    }
}
