package com.chat.room.api.core.frames;

import com.chat.room.api.box.abs.SendPacket;
import com.chat.room.api.core.IoArgs;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

public class SendEntityFrame extends AbsSendPacketFrame {
    /**
     * 未消费的长度
     */
    private final long unConsumeEntityLength;
    private final ReadableByteChannel channel;

    SendEntityFrame(short identifier, long entityLength, ReadableByteChannel channel, SendPacket packet) {
        super((int) Math.min(entityLength, Frame.MAX_CAPACITY), Frame.TYPE_PACKET_ENTITY, Frame.FLAG_NONE, identifier, packet);
        this.unConsumeEntityLength = entityLength - bodyRemaining;
        this.channel = channel;
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        if (packet == null) {
            //已中止当前帧，则填充假数据
            return args.fillEmpty(bodyRemaining);
        }
        return args.readFrom(channel);
    }

    @Override
    public Frame buildNextFrame() {
        if (unConsumeEntityLength == 0) {
            return null;
        }
        return new SendEntityFrame(getBodyIdentifier(), unConsumeEntityLength, channel, packet);
    }
}
