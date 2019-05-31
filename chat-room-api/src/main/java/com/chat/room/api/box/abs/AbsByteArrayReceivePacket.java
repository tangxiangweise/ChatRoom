package com.chat.room.api.box.abs;

import java.io.ByteArrayOutputStream;

/**
 * 定义最基础的基于{@link ByteArrayOutputStream}的输出接受包
 *
 * @param <Entity> 对应的实体泛型，需定义{@link ByteArrayOutputStream}流最终转化的数据实体
 */
public abstract class AbsByteArrayReceivePacket<Entity> extends ReceivePacket<ByteArrayOutputStream, Entity> {

    public AbsByteArrayReceivePacket(long len) {
        super(len);
    }

    /**
     * 创建流操作直接返回{@link ByteArrayOutputStream}流
     *
     * @return
     */
    @Override
    protected final ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream((int) length);
    }

}
