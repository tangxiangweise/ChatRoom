package com.chat.room.api.core.frames;

import com.chat.room.api.core.IoArgs;

import java.io.IOException;

/**
 * 取消发送帧，用于标志某packet取消进行发送数据
 */
public class CancelSendFrame extends AbsSendFrame {

    public CancelSendFrame(short identifier) {
        super(0, Frame.TYPE_COMMAND_SEND_CANCEL, Frame.FLAG_NONE, identifier);
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        return 0;
    }

    @Override
    public Frame nextFrame() {
        return null;
    }
}
