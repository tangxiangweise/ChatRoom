package com.chat.room.client;

import com.chat.room.api.bean.ServerInfo;
import com.chat.room.api.core.Connector;
import com.chat.room.api.utils.CloseUtils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class TCPClient extends Connector {

    public TCPClient(SocketChannel socketChannel) throws IOException {
        setup(socketChannel);
    }

    public void exit() {
        CloseUtils.close(this);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        System.out.println("连接已经关闭，无法读取数据");
    }

    public static TCPClient startWith(ServerInfo info) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()));

        System.out.println("已发起服务器连接，并进入后续流程～");
        System.out.println("客户端信息：" + socketChannel.getLocalAddress().toString());
        System.out.println("服务器信息：" + socketChannel.getRemoteAddress().toString());
        try {
            return new TCPClient(socketChannel);
        } catch (IOException e) {
            System.out.println("连接异常");
            CloseUtils.close(socketChannel);
        }
        return null;
    }

}
