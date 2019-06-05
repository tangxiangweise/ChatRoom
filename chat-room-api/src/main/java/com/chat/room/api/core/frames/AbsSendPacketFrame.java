package com.chat.room.api.core.frames;

import com.chat.room.api.box.abs.SendPacket;
import com.chat.room.api.core.IoArgs;

import java.io.IOException;

public abstract class AbsSendPacketFrame extends AbsSendFrame {

    protected volatile SendPacket<?> packet;

    public AbsSendPacketFrame(int length, byte type, byte flag, short identifier, SendPacket packet) {
        super(length, type, flag, identifier);
        this.packet = packet;
    }

    public final synchronized boolean abort() {
        boolean isSending = isSending();
        if (isSending) {
            fillDirtyDataOnAbort();
        }
        packet = null;
        return !isSending;
    }

    @Override
    public final synchronized Frame nextFrame() {
        return packet == null ? null : buildNextFrame();
    }

    @Override
    public synchronized boolean handle(IoArgs args) throws IOException {
        if (packet == null && !isSending()) {
            //已取消，并且未发送任何数据，直接返回结束，发送下一帧
            return true;
        }
        return super.handle(args);
    }

    protected abstract Frame buildNextFrame();

    protected void fillDirtyDataOnAbort() {

    }

    /**
     * 获取当前对应的发送packet
     *
     * @return
     */
    public synchronized SendPacket getPacket() {
        return packet;
    }

}
