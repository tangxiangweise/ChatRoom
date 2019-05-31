package com.chat.room.api.box;

import com.chat.room.api.box.abs.AbsByteArrayReceivePacket;

import java.io.ByteArrayOutputStream;

public class BytesReceivePacket extends AbsByteArrayReceivePacket<byte[]> {

    public BytesReceivePacket(long len) {
        super(len);
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_BYTES;
    }

    @Override
    protected byte[] buildEntity(ByteArrayOutputStream stream) {
        return stream.toByteArray();
    }

}
