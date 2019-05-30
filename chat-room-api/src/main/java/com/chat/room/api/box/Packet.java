package com.chat.room.api.box;

import java.io.Closeable;

/**
 * 公共的数据封装
 * 提供了类型以及基本的长度定义
 */
public abstract class Packet implements Closeable {

    protected byte type;

    protected int length;

    public byte type() {
        return type;
    }

    public int length() {
        return length;
    }
}
