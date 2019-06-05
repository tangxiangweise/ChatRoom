package com.chat.room.api.core.frames;

import com.chat.room.api.core.IoArgs;

import java.io.IOException;

public abstract class AbsReceiveFrame extends Frame {
    //帧体可读写区域大小
    volatile int bodyRemaining;

    public AbsReceiveFrame(byte[] header) {
        super(header);
        bodyRemaining = getBodyLength();
    }

    @Override
    public synchronized boolean handle(IoArgs args) throws IOException {
        if (bodyRemaining == 0) {
            //已读取所有数据
            return true;
        }
        bodyRemaining -= consumeBody(args);
        return bodyRemaining == 0;
    }

    protected abstract int consumeBody(IoArgs args) throws IOException;

    @Override
    public Frame nextFrame() {
        return null;
    }

    @Override
    public int getConsumableLength() {
        return bodyRemaining;
    }
}
