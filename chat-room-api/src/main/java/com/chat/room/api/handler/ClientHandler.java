package com.chat.room.api.handler;

import com.chat.room.api.box.StringReceivePacket;
import com.chat.room.api.box.abs.Packet;
import com.chat.room.api.box.abs.ReceivePacket;
import com.chat.room.api.constants.Foo;
import com.chat.room.api.core.Connector;
import com.chat.room.api.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;

public class ClientHandler extends Connector {

    private final File cachePath;
    private final String clientInfo;
    private final Executor deliveryPool;
    private final ConnectorCloseChain closeChain = new DefaultPrintConnectorCloseChain();
    private final ConnectorStringPacketChain packetChain = new DefaultNonConnectorStringPacketChain();

    public ClientHandler(SocketChannel socketChannel, File cachePath, Executor deliveryPool) throws IOException {
        this.deliveryPool = deliveryPool;
        this.cachePath = cachePath;
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        setup(socketChannel);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public void exit() {
        CloseUtils.close(this);
        System.out.println("客户端已退出 : " + clientInfo);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        exitBySelf();
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

    private void deliveryStringPacket(StringReceivePacket packet) {
        deliveryPool.execute(() -> packetChain.handle(this, packet));
    }

    public ConnectorStringPacketChain getStringPacketChain() {
        return packetChain;
    }

    public ConnectorCloseChain getCloseChain() {
        return closeChain;
    }

    private void exitBySelf() {
        exit();
    }

    @Override
    protected File createNewReceiveFile() {
        return Foo.createRandomTemp(cachePath);
    }

}
