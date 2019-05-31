package com.chat.room.api.box;

import com.chat.room.api.box.abs.Packet;

/**
 * 字符串发送包
 */
public class StringSendPacket extends BytesSendPacket {

    /**
     * 字符串发送时就是byte数组，所以直接得到byte数组，并按照byte的发送方式
     *
     * @param msg 字符串
     */
    public StringSendPacket(String msg) {
        super(msg.getBytes());
    }

    @Override
    public byte type() {
        return Packet.TYPE_MEMORY_STRING;
    }

}
