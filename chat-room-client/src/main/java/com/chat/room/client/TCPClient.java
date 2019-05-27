package com.chat.room.client;

import com.chat.room.api.bean.ServerInfo;
import com.chat.room.api.utils.CloseUtils;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TCPClient {

    public static void linkWith(ServerInfo info) throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(3000);

        socket.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()), 3000);

        System.out.println("已发起服务器连接，并进入后续流程～");
        System.out.println("客户端信息：" + socket.getLocalAddress() + "p" + socket.getLocalPort());
        System.out.println("服务器信息：" + socket.getInetAddress() + "p" + socket.getPort());
        try {
            ReadHandler readHandler = new ReadHandler(socket.getInputStream());
            readHandler.start();
            //发送接收数据
            write(socket);

            readHandler.exit();
        } catch (Exception e) {
            System.out.println("异常关闭");
        }
        socket.close();
        System.out.println("客户端已退出");
    }

    private static void write(Socket socket) throws IOException {
        //构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        //得到socket输出流，并转换打印流
        OutputStream outputStream = socket.getOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        do {
            //键盘读取一行
            String msg = input.readLine();
            //发送到服务器
            printStream.println(msg);
            if ("bye".equalsIgnoreCase(msg)) {
                break;
            }
        } while (true);

        printStream.close();
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
