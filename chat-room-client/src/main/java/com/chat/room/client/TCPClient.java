package com.chat.room.client;

import com.chat.room.api.bean.ServerInfo;
import com.chat.room.api.box.StringReceivePacket;
import com.chat.room.api.handler.ConnectorHandler;
import com.chat.room.api.handler.ConnectorStringPacketChain;
import com.chat.room.api.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class TCPClient extends ConnectorHandler {

    public TCPClient(SocketChannel socketChannel, File cachePath, boolean pringReceiveString) throws IOException {
        super(socketChannel, cachePath);
        if (pringReceiveString) {
            getStringPacketChain().appendLast(new PrintStringPacketChain());
        }
    }

    private class PrintStringPacketChain extends ConnectorStringPacketChain {
        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket packet) {
            String entity = packet.entity();
            System.out.println(entity);
            return true;
        }
    }

    public static TCPClient startWith(ServerInfo info, File cachePath) throws IOException {
        return startWith(info, cachePath, true);
    }

    public static TCPClient startWith(ServerInfo info, File cachePath, boolean pringReceiveString) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()));

        System.out.println("已发起服务器连接，并进入后续流程～");
        System.out.println("客户端信息：" + socketChannel.getLocalAddress().toString());
        System.out.println("服务器信息：" + socketChannel.getRemoteAddress().toString());
        try {
            return new TCPClient(socketChannel, cachePath, pringReceiveString);
        } catch (IOException e) {
            System.out.println("连接异常");
            CloseUtils.close(socketChannel);
        }
        return null;
    }

}
