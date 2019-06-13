package com.chat.room.api.box;

import com.chat.room.api.box.abs.Packet;
import com.chat.room.api.box.abs.ReceivePacket;

import java.io.OutputStream;

/**
 * 直流接受packet
 */
public class StreamDirectReceivePacket extends ReceivePacket<OutputStream, OutputStream> {

    private OutputStream outputStream;

    public StreamDirectReceivePacket(OutputStream outputStream, long length) {
        super(length);
        //用以读取数据进行输出的输入流
        this.outputStream = outputStream;
    }

    @Override
    protected OutputStream buildEntity(OutputStream stream) {
        return outputStream;
    }

    @Override
    public byte type() {
        return Packet.TYPE_STREAM_DIRECT;
    }

    @Override
    protected OutputStream createStream() {
        return outputStream;
    }

}
