package com.chat.room.api.box.abs;

import java.io.Closeable;
import java.io.IOException;

/**
 * 公共的数据封装
 * 提供了类型以及基本的长度定义
 */
public abstract class Packet<Stream extends Closeable> implements Closeable {
    /**
     * 最大包大小，5个字节满载组成的Long类型
     */
    public static final long MAX_PACKET_SIZE = (((0xFFL) << 32) | ((0xFFL) << 24) | ((0xFFL) << 16) | ((0xFFL) << 8) | (0xFFL));

    //bytes 类型
    public static final byte TYPE_MEMORY_BYTES = 1;
    //string 类型
    public static final byte TYPE_MEMORY_STRING = 2;
    // 文件 类型
    public static final byte TYPE_STREAM_FILE = 3;
    // 长链接流 类型
    public static final byte TYPE_STREAM_DIRECT = 4;


    protected long length;

    private Stream stream;

    /**
     * 返回传输类型
     *
     * @return
     */
    public abstract byte type();

    /**
     * 创建流操作，应当将当前需要传输的数据转化为流
     *
     * @return
     */
    protected abstract Stream createStream();

    public long length() {
        return length;
    }


    protected void closeStream(Stream stream) throws IOException {
        stream.close();
    }

    /**
     * 对外获取当前实例的流操作
     *
     * @return
     */
    public final Stream open() {
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

    /**
     * 头部额外信息，用于携带额外的校验信息等
     *
     * @return
     */
    public byte[] headerInfo() {
        return null;
    }

}
