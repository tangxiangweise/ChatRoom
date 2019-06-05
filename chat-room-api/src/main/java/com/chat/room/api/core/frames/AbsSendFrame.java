package com.chat.room.api.core.frames;

import com.chat.room.api.core.IoArgs;

import java.io.IOException;

public abstract class AbsSendFrame extends Frame {

    volatile byte headerRemaining = Frame.FRAME_HEADER_LENGTH;

    volatile int bodyRemaining;

    public AbsSendFrame(int length, byte type, byte flag, short identifier) {
        super(length, type, flag, identifier);
        this.bodyRemaining = length;
    }

    @Override
    public synchronized boolean handle(IoArgs args) throws IOException {
        try {
            args.limit(headerRemaining + bodyRemaining);
            args.startWriting();
            if (headerRemaining > 0 && args.remained()) {
                headerRemaining -= consumeHeader(args);
            }
            if (headerRemaining == 0 && args.remained() && bodyRemaining > 0) {
                bodyRemaining -= consumeBody(args);
            }
            return headerRemaining == 0 && bodyRemaining == 0;
        } finally {
            args.finishWriting();
        }
    }

    protected abstract int consumeBody(IoArgs args) throws IOException;

    private byte consumeHeader(IoArgs args) {
        int count = headerRemaining;
        int offset = header.length - count;
        return (byte) args.readFrom(header, offset, count);
    }

    /**
     * 是否已经处于发送数据中，如果已经发送了部分数据则返回true
     * 只要头部数据已经消费，则肯定已经处于发送数据中
     *
     * @return
     */
    protected synchronized boolean isSending() {
        return headerRemaining < Frame.FRAME_HEADER_LENGTH;
    }

    @Override
    public int getConsumableLength() {
        return headerRemaining + bodyRemaining;
    }

}
