package com.chat.room.client;

import com.chat.room.api.bean.ServerInfo;
import com.chat.room.api.utils.CloseUtils;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TCPClient {

    private final Socket socket;
    private final ReadHandler readHandler;
    private final PrintStream printStream;

    public TCPClient(Socket socket, ReadHandler readHandler) throws IOException {
        this.socket = socket;
        this.readHandler = readHandler;
        this.printStream = new PrintStream(socket.getOutputStream());
    }

    public void exit() {
        readHandler.exit();
        CloseUtils.close(printStream, socket);
    }

    public void send(String msg) {
        printStream.println(msg);
    }

    public static TCPClient startWith(ServerInfo info) throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(3000);

        socket.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()), 3000);

        System.out.println("已发起服务器连接，并进入后续流程～");
        System.out.println("客户端信息：" + socket.getLocalAddress() + "p" + socket.getLocalPort());
        System.out.println("服务器信息：" + socket.getInetAddress() + "p" + socket.getPort());
        try {
            ReadHandler readHandler = new ReadHandler(socket.getInputStream());
            readHandler.start();
            return new TCPClient(socket, readHandler);
        } catch (IOException e) {
            System.out.println("连接异常");
            CloseUtils.close(socket);
        }
        return null;
    }


    static class ReadHandler extends Thread {

        private boolean done = false;
        private final InputStream inputStream;

        public ReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            super.run();
            try {
                //得到输入流，用于接收数据
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));
                do {
                    String msg;
                    try {
                        msg = socketInput.readLine();
                    } catch (SocketTimeoutException e) {
                        continue;
                    }
                    if (msg == null) {
                        System.out.println("连接已关闭，无法读取数据");
                        break;
                    }
                    System.out.println(msg);
                } while (!done);
                socketInput.close();
            } catch (Exception e) {
                if (!done) {
                    System.out.println("连接异常断开:" + e.getMessage());
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
