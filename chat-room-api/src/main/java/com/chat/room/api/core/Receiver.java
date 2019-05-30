package com.chat.room.api.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {

    boolean receiveAsync(IoArgs args) throws IOException;

    void setReceiveListener(IoArgs.IoArgsEventListener listener);

}
