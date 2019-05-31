package com.chat.room.api.box;

import com.chat.room.api.box.abs.AbsByteArrayReceivePacket;

import java.io.ByteArrayOutputStream;

/**
 * 字符串接受包
 */
public class StringReceivePacket extends AbsByteArrayReceivePacket<String> {

    public StringReceivePacket(long len) {
        super(len);
    }

    @Override
    protected String buildEntity(ByteArrayOutputStream stream) {
        return new String(stream.toByteArray());
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }

}
