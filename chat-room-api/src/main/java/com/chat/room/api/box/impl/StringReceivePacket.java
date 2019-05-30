package com.chat.room.api.box.impl;

import com.chat.room.api.box.ReceivePacket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class StringReceivePacket extends ReceivePacket<ByteArrayOutputStream> {

    private String msg;

    public StringReceivePacket(int len) {
        this.length = len;
    }

    @Override
    protected ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream((int) length);
    }

    @Override
    protected void closeStream(ByteArrayOutputStream stream) throws IOException {
        super.closeStream(stream);
        msg = new String(stream.toByteArray());
    }

    public String message() {
        return msg;
    }

}
