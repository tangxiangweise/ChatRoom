package com.chat.room.api.handler;

import com.chat.room.api.box.abs.Packet;
import com.chat.room.api.box.abs.ReceivePacket;
import com.chat.room.api.constants.Foo;
import com.chat.room.api.core.Connector;
import com.chat.room.api.handler.callback.ClientHandlerCallback;
import com.chat.room.api.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ClientHandler extends Connector {

    private final ClientHandlerCallback handlerCallback;
    private final File cachePath;
    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallback handlerCallback, File cachePath) throws IOException {
        this.handlerCallback = handlerCallback;
        this.cachePath = cachePath;
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        System.out.println("新客户端连接：" + clientInfo);
        setup(socketChannel);
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
        if (packet.type() == Packet.TYPE_MEMORY_STRING) {
            String entity = (String) packet.entity();
            System.out.println(getKey() + " : " + entity);
            handlerCallback.onNewMessageArrived(this, entity);
        }
    }

    private void exitBySelf() {
        exit();
        handlerCallback.onSelfClosed(this);
    }

    @Override
    protected File createNewReceiveFile() {
        return Foo.createRandomTemp(cachePath);
    }

}
