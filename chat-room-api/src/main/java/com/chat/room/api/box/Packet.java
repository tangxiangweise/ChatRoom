package com.chat.room.api.box;

import java.io.Closeable;
import java.io.IOException;

/**
 * 公共的数据封装
 * 提供了类型以及基本的长度定义
 */
public abstract class Packet<T extends Closeable> implements Closeable {

    protected byte type;

    protected long length;

    private T stream;

    public byte type() {
        return type;
    }

    public long length() {
        return length;
    }

    protected abstract T createStream();

    protected void closeStream(T stream) throws IOException {
        stream.close();
    }

    public final T open() {
        if (stream == null) {
            stream = createStream();
        }
        return stream;
    }

    @Override
    public final void close() throws IOException {
        if (stream != null) {
            closeStream(stream);
            stream = null;
        }
    }

}
