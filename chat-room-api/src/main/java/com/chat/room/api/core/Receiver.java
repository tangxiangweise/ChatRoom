package com.chat.room.api.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {

    /**
     * 注册一个read事件
     * @return
     * @throws IOException
     */
    boolean postReceiveAsync() throws IOException;

    void setReceiveListener(IoArgs.IoArgsEventProcessor processor);

    long getLastReadTime();
}
