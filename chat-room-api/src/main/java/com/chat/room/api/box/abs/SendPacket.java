package com.chat.room.api.box.abs;

import java.io.IOException;
import java.io.InputStream;

/**
 * 发送包的定义
 */
public abstract class SendPacket<T extends InputStream> extends Packet<T> {

    private boolean isCanceled;

    public boolean isCanceled() {
        return isCanceled;
    }

    /**
     * 设置取消发送标记
     */
    public void cancel() {
        isCanceled = true;
    }

    /**
     * <p>
     * 我们利用该方法不断得到直流传输的可发送数据量，从而不断生成Frame
     * </p>
     * 缺陷 ：对于流的数据量大于int有效值范围外侧则得不到准确值
     * <p>
     * 一般情况下，发送数据包不使用该方法，而使用总长度进行运算
     * 对于直流传输需要使用该方法，因为对于直流而言没有最大长度
     *
     * @return 默认返回stream的可用数据量：0代表无数据可输出了
     */
    public int available() {
        InputStream stream = open();
        try {
            int available = stream.available();
            if (available < 0) {
                return 0;
            }
            return available;
        } catch (IOException e) {
            return 0;
        }
    }

}
