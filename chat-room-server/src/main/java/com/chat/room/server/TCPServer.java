package com.chat.room.server;

import com.chat.room.api.bean.Group;
import com.chat.room.api.box.StringReceivePacket;
import com.chat.room.api.constants.Foo;
import com.chat.room.api.core.Connector;
import com.chat.room.api.core.schedule.IdleTimeoutScheduleJob;
import com.chat.room.api.core.schedule.ScheduleJob;
import com.chat.room.api.handler.ConnectorCloseChain;
import com.chat.room.api.handler.ConnectorHandler;
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
import java.util.concurrent.TimeUnit;

public class TCPServer implements ServerAcceptor.AcceptListener, Group.GroupMessageAdapter {

    private final int port;
    private final File cachePath;
    private final List<ConnectorHandler> connectorHandlers = new ArrayList<>();
    private final Map<String, Group> groups = new HashMap<>();
    private ServerAcceptor acceptor;
    private ServerSocketChannel server;

    public TCPServer(int port, File cachePath) {
        this.port = port;
        this.cachePath = cachePath;
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
            System.out.println("acceptor.start()");
            if (acceptor.awaitRunning()) {
                System.out.println("服务器准备就绪～");
                System.out.println("服务器信息：" + server.getLocalAddress().toString());
            } else {
                System.out.println("启动异常");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 关闭操作
     */
    public void stop() {
        if (acceptor != null) {
            acceptor.exit();
        }
        ConnectorHandler[] handlers;
        synchronized (connectorHandlers) {
            handlers = connectorHandlers.toArray(new ConnectorHandler[0]);
            connectorHandlers.clear();
        }
        for (ConnectorHandler connectorHandler : handlers) {
            connectorHandler.exit();
        }
        CloseUtils.close(server);
    }

    public void broadcast(String msg) {
        msg = " 系统通知 : " + msg;
        ConnectorHandler[] handlers;
        synchronized (connectorHandlers) {
            handlers = connectorHandlers.toArray(new ConnectorHandler[0]);
        }
        for (ConnectorHandler connectorHandler : handlers) {
            sendMessageToClient(connectorHandler, msg);
        }
    }

    @Override
    public void sendMessageToClient(ConnectorHandler handler, String msg) {
        handler.send(msg);
    }

    @Override
    public void onNewSocketArrived(SocketChannel channel) {
        try {
            ConnectorHandler connectorHandler = new ConnectorHandler(channel, cachePath);
            System.out.println(connectorHandler.getClientInfo() + " : Connected");
            //添加收到消息的处理责任链
            connectorHandler.getStringPacketChain().appendLast(new ParseCommandConnectorStringPacketChain());
            //添加关闭链接时的责任链
            connectorHandler.getCloseChain().appendLast(new RemoveQueueOnConnectorClosedChain());

            ScheduleJob scheduleJob = new IdleTimeoutScheduleJob(5, TimeUnit.SECONDS, connectorHandler);
            connectorHandler.schedule(scheduleJob);

            synchronized (connectorHandlers) {
                connectorHandlers.add(connectorHandler);
                System.out.println("当前客户端数量 : " + connectorHandlers.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("客户端链接异常 : " + e.getMessage());
        }
    }

    private class RemoveQueueOnConnectorClosedChain extends ConnectorCloseChain {
        @Override
        protected boolean consume(ConnectorHandler handler, Connector connector) {
            synchronized (connectorHandlers) {
                connectorHandlers.remove(handler);
                //移除群聊客户端
                Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                group.removeMember(handler);
            }
            return true;
        }
    }

    private class ParseCommandConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket packet) {
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
        protected boolean consumeAgain(ConnectorHandler handler, StringReceivePacket packet) {
            sendMessageToClient(handler, packet.entity());
            return true;
        }
    }

}
