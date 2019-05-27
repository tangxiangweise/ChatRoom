package com.chat.room.server;

import com.chat.room.api.handler.ClientHandler;
import com.chat.room.api.handler.callback.ClientHandlerCallback;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandlerCallback {

    private final int port;

    private ClientListener listener;

    private List<ClientHandler> clientHandlers = new ArrayList<>();

    private final ExecutorService forwardingThreadPoolExecutor;

    public TCPServer(int port) {
        this.port = port;
        this.forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            ClientListener clientListener = new ClientListener(port);
            this.listener = clientListener;
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
        private ServerSocket server;
        private boolean done = false;

        public ClientListener(int port) throws IOException {
            this.server = new ServerSocket(port);
            System.out.println("服务器信息：" + server.getInetAddress() + " p : " + server.getLocalPort());
        }

        @Override
        public void run() {
            super.run();
            System.out.println("服务器准备就绪～");
            do {
                Socket client;
                try {
                    client = server.accept();
                } catch (IOException e) {
                    continue;
                }
                try {
                    ClientHandler clientHandler = new ClientHandler(client, TCPServer.this);
                    clientHandler.readToPrint();
                    synchronized (TCPServer.this) {
                        clientHandlers.add(clientHandler);
                    }
                } catch (IOException e) {
                    System.out.println("客户端连接异常" + e.getMessage());
                }
            } while (!done);
            System.out.println("服务器已关闭");
        }

        public void exit() {
            done = true;
            try {
                server.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public synchronized void onSelfClosed(ClientHandler handler) {
        clientHandlers.remove(handler);
    }

    @Override
    public void onNewMessageArrived(final ClientHandler handler, final String msg) {
        System.out.println("Received-" + handler.getClientInfo() + " : " + msg);
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
