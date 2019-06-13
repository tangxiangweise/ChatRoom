package com.chat.room.api.box;

import com.chat.room.api.box.abs.Packet;
import com.chat.room.api.box.abs.SendPacket;

import java.io.InputStream;

/**
 * 直流发送packet
 */
public class StreamDirectSendPacket extends SendPacket<InputStream> {

    private InputStream inputStream;

    public StreamDirectSendPacket(InputStream inputStream) {
        //用以读取数据进行输出的输入流
        this.inputStream = inputStream;
        //长度不固定，所以为最大值
        this.length = MAX_PACKET_SIZE;
    }

    @Override
    public byte type() {
        return Packet.TYPE_STREAM_DIRECT;
    }

    @Override
    protected InputStream createStream() {
        return inputStream;
    }
}
