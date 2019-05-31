package com.chat.room.api.box;

import com.chat.room.api.box.abs.Packet;
import com.chat.room.api.box.abs.SendPacket;

import java.io.ByteArrayInputStream;

/**
 * 纯byte数组发送包
 */
public class BytesSendPacket extends SendPacket<ByteArrayInputStream> {

    private final byte[] bytes;

    public BytesSendPacket(byte[] bytes) {
        this.bytes = bytes;
        this.length = bytes.length;
    }

    @Override
    protected ByteArrayInputStream createStream() {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public byte type() {
        return Packet.TYPE_MEMORY_BYTES;
    }

}
