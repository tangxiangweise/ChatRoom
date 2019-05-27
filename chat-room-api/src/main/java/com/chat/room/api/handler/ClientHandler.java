package com.chat.room.api.handler;

import com.chat.room.api.handler.callback.ClientHandlerCallback;
import com.chat.room.api.utils.CloseUtils;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {

    private final Socket socket;
    private final ClientReadHandler readHandler;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerCallback handlerCallback;
    private final String clientInfo;

    public ClientHandler(Socket socket, ClientHandlerCallback handlerCallback) throws IOException {
        this.socket = socket;
        this.handlerCallback = handlerCallback;
        this.readHandler = new ClientReadHandler(socket.getInputStream());
        this.writeHandler = new ClientWriteHandler(socket.getOutputStream());
        this.clientInfo = "A[" + socket.getInetAddress().getHostAddress() + "]  P[" + socket.getPort() + "]";
        System.out.println("新客户端连接：" + clientInfo);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public void send(String msg) {
        writeHandler.send(msg);
    }

    public void exit() {
        readHandler.exit();
        writeHandler.exit();
        CloseUtils.close(socket);
        System.out.println("客户端已退出 : " + socket.getInetAddress() + " p : " + socket.getPort());
    }

    public void readToPrint() {
        readHandler.start();
    }

    private void exitBySelf() {
        exit();
        handlerCallback.onSelfClosed(this);
    }


    class ClientWriteHandler {

        private boolean done = false;
        private final PrintStream printStream;
        private final ExecutorService executorService;

        public ClientWriteHandler(OutputStream outputStream) {
            this.printStream = new PrintStream(outputStream);
            this.executorService = Executors.newSingleThreadExecutor();
        }

        public void send(String msg) {
            if (done) {
                return;
            }
            executorService.execute(new WriteRunnable(msg));
        }

        void exit() {
            done = true;
            executorService.shutdownNow();
            CloseUtils.close(printStream);
        }

        class WriteRunnable implements Runnable {

            private final String msg;

            public WriteRunnable(String msg) {
                this.msg = msg;
            }

            @Override
            public void run() {
                try {
                    if (ClientWriteHandler.this.done) {
                        return;
                    }
                    ClientWriteHandler.this.printStream.println(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    class ClientReadHandler extends Thread {

        private boolean done = false;
        private final InputStream inputStream;

        public ClientReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            super.run();
            try {
                //得到输入流，用于接收数据
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));
                do {
                    String msg = socketInput.readLine();
                    if (msg == null) {
                        System.out.println("客户端已无法读取数据");
                        ClientHandler.this.exitBySelf();
                        break;
                    }
                    System.out.println(msg);
                    handlerCallback.onNewMessageArrived(ClientHandler.this, msg);
                } while (!done);
                socketInput.close();
            } catch (Exception e) {
                if (!done) {
                    System.out.println("连接异常断开");
                    ClientHandler.this.exitBySelf();
                }
            } finally {
                CloseUtils.close(inputStream);
            }
        }

        void exit() {
            done = true;
            CloseUtils.close(inputStream);
        }
    }

}
