package com.chat.room.server;

import com.chat.room.api.handler.ClientHandler;
import com.chat.room.api.handler.callback.ClientHandlerCallback;
import com.chat.room.api.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandlerCallback {

    private final int port;
    private Selector selector;
    private final File cachePath;
    private ClientListener listener;
    private ServerSocketChannel server;
    private List<ClientHandler> clientHandlers = new ArrayList<>();
    private final ExecutorService forwardingThreadPoolExecutor;

    public TCPServer(int port, File cachePath) {
        this.port = port;
        this.cachePath = cachePath;
        this.forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            selector = Selector.open();
            ServerSocketChannel server = ServerSocketChannel.open();
            //设置为非阻塞
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(port));
            //注册客户端连接到达监听
            server.register(selector, SelectionKey.OP_ACCEPT);
            this.server = server;
            System.out.println("服务器信息：" + server.getLocalAddress().toString());

            ClientListener clientListener = this.listener = new ClientListener();
            clientListener.start();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        if (listener != null) {
            listener.exit();
        }
        CloseUtils.close(server, selector);
        synchronized (TCPServer.this) {
            for (ClientHandler clientHandler : clientHandlers) {
                clientHandler.exit();
            }
            clientHandlers.clear();
        }

        forwardingThreadPoolExecutor.shutdownNow();
    }

    public synchronized void broadcast(String msg) {
        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.send(msg);
        }
    }

    private class ClientListener extends Thread {

        private boolean done = false;

        @Override
        public void run() {
            super.run();

            Selector selector = TCPServer.this.selector;
            System.out.println("服务器准备就绪～");
            do {
                //得到客户端
                try {
                    if (selector.select() == 0) {
                        if (done) {
                            break;
                        }
                        continue;
                    }
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        if (done) {
                            break;
                        }
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        //检查当前key的状态是否是我们关注的
                        if (key.isAcceptable()) {
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                            //非阻塞状态拿到客户端连接
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            try {
                                ClientHandler clientHandler = new ClientHandler(socketChannel, TCPServer.this, cachePath);
                                synchronized (TCPServer.this) {
                                    clientHandlers.add(clientHandler);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println("客户端连接异常" + e.getMessage());
                            }

                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (!done);
            System.out.println("服务器已关闭");
        }

        public void exit() {
            done = true;
            //唤醒当前阻塞
            selector.wakeup();
        }
    }


    @Override
    public synchronized void onSelfClosed(ClientHandler handler) {
        clientHandlers.remove(handler);
    }

    @Override
    public void onNewMessageArrived(final ClientHandler handler, final String msg) {
        //异步操作
        forwardingThreadPoolExecutor.execute(() -> {
            synchronized (TCPServer.this) {
                for (ClientHandler clientHandler : clientHandlers) {
                    if (clientHandler.equals(handler)) {
                        continue;
                    }
                    //对其他客户端发送消息
                    clientHandler.send(msg);
                }
            }
        });

    }
}
