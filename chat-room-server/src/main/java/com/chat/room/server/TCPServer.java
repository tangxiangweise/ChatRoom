package com.chat.room.server;

import com.chat.room.api.bean.Group;
import com.chat.room.api.box.StringReceivePacket;
import com.chat.room.api.constants.Foo;
import com.chat.room.api.core.Connector;
import com.chat.room.api.handler.ClientHandler;
import com.chat.room.api.handler.ConnectorCloseChain;
import com.chat.room.api.handler.ConnectorStringPacketChain;
import com.chat.room.api.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ServerAcceptor.AcceptListener, Group.GroupMessageAdapter {

    private final int port;
    private final File cachePath;
    private final ExecutorService deliveryPool;
    private final List<ClientHandler> clientHandlers = new ArrayList<>();
    private final Map<String, Group> groups = new HashMap<>();
    private ServerAcceptor acceptor;
    private ServerSocketChannel server;

    public TCPServer(int port, File cachePath) {
        this.port = port;
        this.cachePath = cachePath;
        this.deliveryPool = Executors.newSingleThreadExecutor();
        this.groups.put(Foo.DEFAULT_GROUP_NAME, new Group(Foo.DEFAULT_GROUP_NAME, this));
    }

    public boolean start() {
        try {
            ServerAcceptor acceptor = new ServerAcceptor(this);

            ServerSocketChannel server = ServerSocketChannel.open();
            //设置为非阻塞
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(port));
            //注册客户端连接到达监听
            server.register(acceptor.getSelector(), SelectionKey.OP_ACCEPT);
            this.server = server;
            this.acceptor = acceptor;
            acceptor.start();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        if (acceptor != null) {
            acceptor.exit();
        }
        synchronized (clientHandlers) {
            for (ClientHandler clientHandler : clientHandlers) {
                clientHandler.exit();
            }
            clientHandlers.clear();
        }
        CloseUtils.close(server);
        deliveryPool.shutdownNow();
    }

    public void broadcast(String msg) {
        msg = " 系统通知 : " + msg;
        synchronized (clientHandlers) {
            for (ClientHandler clientHandler : clientHandlers) {
                sendMessageToClient(clientHandler, msg);
            }
        }
    }

    @Override
    public void sendMessageToClient(ClientHandler handler, String msg) {
        handler.send(msg);
    }

    @Override
    public void onNewSocketArrived(SocketChannel channel) {
        try {
            ClientHandler clientHandler = new ClientHandler(channel, cachePath, deliveryPool);
            System.out.println(clientHandler.getClientInfo() + " : Connected");

            clientHandler.getStringPacketChain().appendLast(new ParseCommandConnectorStringPacketChain());

            clientHandler.getCloseChain().appendLast(new RemoveQueueOnConnectorClosedChain());

            synchronized (clientHandlers) {
                clientHandlers.add(clientHandler);
                System.out.println("当前客户端数量 : " + clientHandlers.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("客户端链接异常 : " + e.getMessage());
        }
    }

    private class RemoveQueueOnConnectorClosedChain extends ConnectorCloseChain {
        @Override
        protected boolean consume(ClientHandler handler, Connector connector) {
            synchronized (clientHandlers) {
                clientHandlers.remove(handler);
                //移除群聊客户端
                Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                group.removeMember(handler);
            }
            return true;
        }
    }

    private class ParseCommandConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ClientHandler handler, StringReceivePacket packet) {
            String entity = packet.entity();
            if (entity.startsWith(Foo.COMMAND_GROUP_JOIN)) {
                Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                if (group.addMember(handler)) {
                    sendMessageToClient(handler, "Join Group : " + group.getName());
                }
                return true;
            } else if (entity.startsWith(Foo.COMMAND_GROUP_LEAVE)) {
                Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                if (group.removeMember(handler)) {
                    sendMessageToClient(handler, "Leave Group : " + group.getName());
                }
                return true;
            }
            return false;
        }

        @Override
        protected boolean consumeAgain(ClientHandler handler, StringReceivePacket packet) {
            sendMessageToClient(handler, packet.entity());
            return true;
        }
    }

}
