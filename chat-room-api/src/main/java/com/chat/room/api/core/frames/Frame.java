package com.chat.room.api.core.frames;

import com.chat.room.api.core.IoArgs;

import java.io.IOException;

public abstract class Frame {

    public static final int FRAME_HEADER_LENGTH = 6;

    public static final int MAX_CAPACITY = 64 * 1024 - 1;

    public static final byte TYPE_PACKET_HEADER = 11;

    public static final byte TYPE_PACKET_ENTITY = 12;

    public static final byte TYPE_COMMAND_SEND_CANCEL = 41;

    public static final byte TYPE_COMMAND_RECEIVE_REJECT = 42;

    public static final byte FLAG_NONE = 0;

    protected final byte[] header = new byte[FRAME_HEADER_LENGTH];

    public Frame(int length, byte type, byte flag, short identifier) {
        if (length < 0 || length > MAX_CAPACITY) {
            throw new RuntimeException("");
        } else if (identifier < 1 || identifier > 255) {
            throw new RuntimeException("");
        }
        header[0] = (byte) (length >> 8);
        header[1] = (byte) (length);
        header[2] = type;
        header[3] = flag;
        header[4] = (byte) identifier;
        header[5] = 0;
    }

    public Frame(byte[] header) {
        System.arraycopy(header, 0, this.header, 0, FRAME_HEADER_LENGTH);
    }

    public int getBodyLength() {
        /**
         * 01000000 原值
         * 11111111 11111111 11111111 01000000 转int自动补齐高位
         * 00000000 00000000 00000000 11111111 0xFF
         * 00000000 00000000 00000000 01000000 &操作结果
         */
        return ((((int) header[0] & 0xFF) << 8) | ((int) header[1] & 0xFF));
    }

    public byte getBodyFlag() {
        return header[3];
    }

    public byte getBodyType() {
        return header[2];
    }

    public short getBodyIdentifier() {
        return (short) (((short) header[4]) & 0xFF);
    }

    public abstract boolean handle(IoArgs args) throws IOException;

    /**
     * 基于当前帧尝试构建下一份待消费的帧
     * 64MB 64KB 1024+1 6000+
     *
     * @return NULL：没有待消费的帧
     */
    public abstract Frame nextFrame();

    public abstract int getConsumableLength();

}
