package com.chat.room.api.core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender  extends Closeable {
    /**
     *
     * @param args 发送的数据
     * @param listener 发送状态通过回调
     * @return
     * @throws IOException
     */
    boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) throws IOException;
}
