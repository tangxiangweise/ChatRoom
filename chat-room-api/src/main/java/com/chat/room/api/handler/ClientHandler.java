package com.chat.room.api.handler;

import com.chat.room.api.core.Connector;
import com.chat.room.api.handler.callback.ClientHandlerCallback;
import com.chat.room.api.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ClientHandler extends Connector {

    private final ClientHandlerCallback handlerCallback;
    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallback handlerCallback) throws IOException {
        this.handlerCallback = handlerCallback;
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
    protected void onReceiveNewMessage(String msg) {
        super.onReceiveNewMessage(msg);
        handlerCallback.onNewMessageArrived(this, msg);
    }

    private void exitBySelf() {
        exit();
        handlerCallback.onSelfClosed(this);
    }


}
