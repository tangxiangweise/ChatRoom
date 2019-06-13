package com.chat.room.api.handler;

import com.chat.room.api.box.StringReceivePacket;
import com.chat.room.api.box.abs.Packet;
import com.chat.room.api.box.abs.ReceivePacket;
import com.chat.room.api.constants.Foo;
import com.chat.room.api.core.Connector;
import com.chat.room.api.core.IoContext;
import com.chat.room.api.utils.CloseUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;

public class ConnectorHandler extends Connector {

    private final File cachePath;
    private final String clientInfo;
    private final ConnectorCloseChain closeChain = new DefaultPrintConnectorCloseChain();
    private final ConnectorStringPacketChain packetChain = new DefaultNonConnectorStringPacketChain();

    public ConnectorHandler(SocketChannel socketChannel, File cachePath) throws IOException {
        this.cachePath = cachePath;
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        setup(socketChannel);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    /**
     * 外部调用的退出操作
     */
    public void exit() {
        CloseUtils.close(this);
    }

    /**
     * 内部监测到链接断开的回调
     *
     * @param channel
     */
    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        closeChain.handle(this, this);
    }

    @Override
    protected File createNewReceiveFile(long length, byte[] headerInfo) {
        return Foo.createRandomTemp(cachePath);
    }

    @Override
    protected OutputStream createNewReceiveDirectOutputStream(long length, byte[] headerInfo) {
        //服务器默认创建一个内存存储ByteArrayOutputStream
        return new ByteArrayOutputStream();
    }

    @Override
    protected void onReceivedPacket(ReceivePacket packet) {
        super.onReceivedPacket(packet);
        switch (packet.type()) {
            case Packet.TYPE_MEMORY_STRING: {
                deliveryStringPacket((StringReceivePacket) packet);
                break;
            }
            default: {
                System.out.println(" New Packet : " + packet.type() + " length : " + packet.length());
            }
        }
    }

    /**
     * 避免阻塞当前的数据读取线程调度，则单独交给另外一个调度线程进行数据调度
     *
     * @param packet
     */
    private void deliveryStringPacket(StringReceivePacket packet) {
        IoContext.get().getScheduler().delivery(() -> packetChain.handle(this, packet));
    }

    /**
     * 获取当前链接的消费处理责任链 链头
     *
     * @return
     */
    public ConnectorStringPacketChain getStringPacketChain() {
        return packetChain;
    }

    /**
     * 获取当前链接的关闭处理责任链 链头
     *
     * @return
     */
    public ConnectorCloseChain getCloseChain() {
        return closeChain;
    }


}
