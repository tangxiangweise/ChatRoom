package com.chat.room.api.box;

/**
 * 发送包的定义
 */
public abstract class SendPacket extends Packet {

    private boolean isCanceled;

    public abstract byte[] bytes();

    public boolean isCanceled() {
        return isCanceled;
    }

}
