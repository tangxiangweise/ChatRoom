package com.chat.room.api.core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {

    boolean postSendAsync() throws IOException;

    void setSendListener(IoArgs.IoArgsEventProcessor processor);
}
