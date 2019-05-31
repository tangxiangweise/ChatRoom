package com.chat.room.api.box.abs;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 接受包的定义
 */
public abstract class ReceivePacket<Stream extends OutputStream, Entity> extends Packet<Stream> {

    /**
     * 定义当前接受包最终的实体
     */
    private Entity entity;

    public ReceivePacket(long len) {
        this.length = len;
    }

    /**
     * 得到最终接收到的数据实体
     *
     * @return
     */
    public Entity entity() {
        return entity;
    }

    /**
     * 根据接收到的流转化为对应的实体
     *
     * @param stream
     * @return
     */
    protected abstract Entity buildEntity(Stream stream);

    /**
     * 先关闭流，随后将流的内容转化为对应的实体
     *
     * @param stream
     * @throws IOException
     */
    @Override
    protected void closeStream(Stream stream) throws IOException {
        super.closeStream(stream);
        entity = buildEntity(stream);
    }

}
