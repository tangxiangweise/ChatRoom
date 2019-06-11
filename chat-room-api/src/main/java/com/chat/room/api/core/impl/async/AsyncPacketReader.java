package com.chat.room.api.core.impl.async;

import com.chat.room.api.box.abs.SendPacket;
import com.chat.room.api.core.IoArgs;
import com.chat.room.api.core.ds.BytePriorityNode;
import com.chat.room.api.core.frames.*;

import java.io.Closeable;
import java.io.IOException;

public class AsyncPacketReader implements Closeable {

    private volatile IoArgs args = new IoArgs();

    private final PacketProvider provider;

    private volatile BytePriorityNode<Frame> node;

    private volatile int nodeSize = 0;

    private short lastIdentifier = 0;

    AsyncPacketReader(PacketProvider provider) {
        this.provider = provider;
    }

    /**
     * 关闭packet
     * 并取消当前packet所有帧
     * 如果头帧还未发送直接取消即可
     * 已发送部分数据构建取消帧发送
     * @param packet
     */
    public synchronized void cancel(SendPacket packet) {
        if (nodeSize == 0) {
            return;
        }
        for (BytePriorityNode<Frame> x = node, before = null; x != null; before = x, x = x.next) {
            Frame frame = x.item;
            if (frame instanceof AbsSendPacketFrame) {
                AbsSendPacketFrame packetFrame = (AbsSendPacketFrame) frame;
                if (packetFrame.getPacket() == packet) {
                    boolean removable = packetFrame.abort();
                    if (removable) {
                        removeFrame(x, before);
                        if (packetFrame instanceof SendHeaderFrame) {
                            //头帧，并且未被发送任何数据，直接取消后不需要添加取消发送帧
                            break;
                        }
                    }
                    //添加终止帧，通知到接受方
                    CancelSendFrame cancelSendFrame = new CancelSendFrame(packetFrame.getBodyIdentifier());
                    appendNewFrame(cancelSendFrame);
                    //意外终止，返回失败
                    provider.completedPacket(packet, false);
                    break;
                }
            }
        }

    }


    /**
     * 请求从{@link #provider} 队列中拿一份packet进行发送
     *
     * @return 如果当前 Reader中有可以用于网络发送的数据，则返回true
     */
    public boolean requestTakePacket() {
        synchronized (this) {
            if (nodeSize >= 1) {
                //上一个 packet 还未完全发送
                return true;
            }
        }
        SendPacket packet = provider.takePacket();
        if (packet != null) {
            short identifier = generateIdentifier();
            SendHeaderFrame frame = new SendHeaderFrame(identifier, packet);
            appendNewFrame(frame);
        }
        synchronized (this) {
            return nodeSize != 0;
        }
    }

    /**
     * 请求发送一个心跳包
     * @return
     */
    boolean requestSendHeartbeatFrame() {
        synchronized (this) {
            for (BytePriorityNode<Frame> x = node; x != null; x = x.next) {
                Frame frame = x.item;
                if (frame.getBodyType() == Frame.TYPE_COMMAND_HEARTBEAT) {
                    return false;
                }
            }
            appendNewFrame(new HeartbeatSendFrame());
            return true;
        }
    }


    private short generateIdentifier() {
        short identifier = ++lastIdentifier;
        if (identifier == 255) {
            lastIdentifier = 0;
        }
        return identifier;
    }

    /**
     * 关闭当前Reader，关闭时应关闭所有的frame对应的packet
     */
    @Override
    public synchronized void close() {
        while (node != null) {
            Frame frame = node.item;
            if (frame instanceof AbsSendPacketFrame) {
                SendPacket packet = ((AbsSendPacketFrame) frame).getPacket();
                provider.completedPacket(packet, false);
            }
            node = node.next;
        }
        nodeSize = 0;
        node = null;
    }

    /**
     * 往IoArgs填充数据
     * @return
     */
    public IoArgs fillData() {
        Frame currentFrame = getCurrentFrame();
        if (currentFrame == null) {
            return null;
        }
        try {
            if (currentFrame.handle(args)) {
                // 消费完本帧
                //尝试基于本帧构建后续帧
                Frame nextFrame = currentFrame.nextFrame();
                if (nextFrame != null) {
                    appendNewFrame(nextFrame);
                } else if (currentFrame instanceof SendEntityFrame) {
                    // 没有下一帧 末尾实体帧 通知完成
                    provider.completedPacket(((SendEntityFrame) currentFrame).getPacket(), true);
                }
                //从链头弹出
                popCurrentFrame();
            }
            return args;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 从链表删除节点
     * @param removeNode 删除节点
     * @param before 删除节点前置节点
     */
    private synchronized void removeFrame(BytePriorityNode<Frame> removeNode, BytePriorityNode<Frame> before) {
        if (before == null) {
            node = removeNode.next;
        } else {
            before.next = removeNode.next;
        }
        nodeSize--;
        if (node == null) {
            requestTakePacket();
        }
    }

    /**
     * 往链表添加新的帧
     * @param frame
     */
    private synchronized void appendNewFrame(Frame frame) {
        BytePriorityNode<Frame> newNode = new BytePriorityNode<>(frame);
        if (node != null) {
            //使用优先级别添加到链表
            node.appendWithPriority(newNode);
        } else {
            node = newNode;
        }
        nodeSize++;
    }

    /**
     * 弹出头帧 如果下一帧为null，尝试构建新的帧
     */
    private synchronized void popCurrentFrame() {
        node = node.next;
        nodeSize--;
        if (node == null) {
            requestTakePacket();
        }
    }

    /**
     * 获取头帧
     * @return
     */
    private synchronized Frame getCurrentFrame() {
        if (node == null) {
            return null;
        }
        return node.item;
    }

    interface PacketProvider {

        SendPacket takePacket();

        void completedPacket(SendPacket packet, boolean isSucceed);

    }

}
