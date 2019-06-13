package com.chat.room.api.core.impl.async;

import com.chat.room.api.box.abs.ReceivePacket;
import com.chat.room.api.core.IoArgs;
import com.chat.room.api.core.frames.*;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.HashMap;

/**
 * 写数据到packet中
 */
public class AsyncPacketWriter implements Closeable {

    private final AsyncPacketWriter.PacketProvider provider;

    private final HashMap<Short, PacketModel> packetMap = new HashMap<>();

    private final IoArgs args = new IoArgs();

    private volatile Frame frameTemp;


    AsyncPacketWriter(AsyncPacketWriter.PacketProvider provider) {
        this.provider = provider;
    }

    /**
     * 构建一份数据容纳封装
     * 当前帧如果没有则返回至少6字节长度的IoArgs
     * 如果当前帧有，则返回当前帧未消费完成的区间
     *
     * @return
     */
    public synchronized IoArgs takeIoArgs() {
        args.limit(frameTemp == null ? Frame.FRAME_HEADER_LENGTH : frameTemp.getConsumableLength());
        return args;
    }

    /**
     * 消费IoArgs中的数据
     *
     * @param args
     */
    public synchronized void consumeIoArgs(IoArgs args) {
        if (frameTemp == null) {
            Frame temp;
            do {
                temp = buildNewFrame(args);
            } while (temp == null && args.remained());
            if (temp == null) {
                return;
            }
            frameTemp = temp;
            if (!args.remained()) {
                return;
            }
        }
        Frame currentFrame = frameTemp;
        do {
            try {
                if (currentFrame.handle(args)) {
                    if (currentFrame instanceof ReceiveHeaderFrame) {
                        ReceiveHeaderFrame headerFrame = (ReceiveHeaderFrame) currentFrame;
                        ReceivePacket packet = provider.takePacket(headerFrame.getPacketType(), headerFrame.getPacketLength(), headerFrame.getPacketHeaderInfo());
                        appendNewPacket(headerFrame.getBodyIdentifier(), packet);
                    } else if (currentFrame instanceof ReceiveEntityFrame) {
                        completeEntityFrame((ReceiveEntityFrame) currentFrame);
                    }
                    frameTemp = null;
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (args.remained());

    }

    /**
     * 计算packet数据是否填充完 完成回调完成接口
     * @param frame
     */
    private void completeEntityFrame(ReceiveEntityFrame frame) {
        synchronized (packetMap) {
            short identifier = frame.getBodyIdentifier();
            int length = frame.getBodyLength();
            PacketModel model = packetMap.get(identifier);
            if (model == null) {
                return;
            }
            model.unreceivedLength -= length;
            if (model.unreceivedLength <= 0) {
                provider.completedPacket(model.packet, true);
                packetMap.remove(identifier);
            }
        }
    }

    /**
     * 创建一个新的PacketModel
     * @param identifier key
     * @param packet
     */
    private void appendNewPacket(short identifier, ReceivePacket packet) {
        synchronized (packetMap) {
            PacketModel model = new PacketModel(packet);
            packetMap.put(identifier, model);
        }
    }

    /**
     * 构建新的帧
     * @param args
     * @return
     */
    private Frame buildNewFrame(IoArgs args) {
        AbsReceiveFrame frame = ReceiveFrameFactory.createInstance(args);
        if (frame instanceof CancelReceiveFrame) {
            cancelReceivePacket(frame.getBodyIdentifier());
            return null;
        } else if (frame instanceof HeartbeatReceiveFrame) {
            provider.onReceivedHeartbeat();
            return null;
        } else if (frame instanceof ReceiveEntityFrame) {
            WritableByteChannel channel = getPacketChannel(frame.getBodyIdentifier());
            ((ReceiveEntityFrame) frame).bindPacketChannel(channel);
        }
        return frame;
    }

    /**
     * 获取packet通道
     * @param identifier
     * @return
     */
    private WritableByteChannel getPacketChannel(short identifier) {
        synchronized (packetMap) {
            PacketModel model = packetMap.get(identifier);
            return model == null ? null : model.channel;
        }
    }

    /**
     * 取消接受packet
     * @param identifier 标识
     */
    private void cancelReceivePacket(short identifier) {
        synchronized (packetMap) {
            PacketModel model = packetMap.get(identifier);
            if (model != null) {
                ReceivePacket packet = model.packet;
                provider.completedPacket(packet, false);
            }
        }
    }

    @Override
    public synchronized void close() {
        synchronized (packetMap) {
            Collection<PacketModel> values = packetMap.values();
            for (PacketModel value : values) {
                provider.completedPacket(value.packet, false);
            }
            packetMap.clear();
        }
    }

    /**
     * Packet 提供者
     */
    interface PacketProvider {
        /**
         * 拿packet操作
         *
         * @param type       packet 类型
         * @param length     packet 长度
         * @param headerInfo
         * @return 通过类型、长度、描述等信息得到一份接受packet
         */
        ReceivePacket takePacket(byte type, long length, byte[] headerInfo);

        /**
         * 结束一份packet
         *
         * @param packet    接收包
         * @param isSucceed 是否接收完成
         */
        void completedPacket(ReceivePacket packet, boolean isSucceed);

        void onReceivedHeartbeat();
    }

    /**
     * 对于接受包的简单封装
     * 用以提供packet、通道、未接收长度信息存储
     */
    static class PacketModel {
        final ReceivePacket packet;
        final WritableByteChannel channel;
        volatile long unreceivedLength;

        public PacketModel(ReceivePacket<?, ?> packet) {
            this.packet = packet;
            this.channel = Channels.newChannel(packet.open());
            this.unreceivedLength = packet.length();
        }
    }

}
