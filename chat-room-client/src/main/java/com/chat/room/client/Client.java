package com.chat.room.client;

import com.chat.room.api.bean.ServerInfo;
import com.chat.room.api.box.FileSendPacket;
import com.chat.room.api.constants.Foo;
import com.chat.room.api.core.IoContext;
import com.chat.room.api.core.impl.IoSelectorProvider;

import java.io.*;

public class Client {

    public static void main(String[] args) throws IOException {

        File cachePath = Foo.getCacheDir("client");
        IoContext.setup().ioProvider(new IoSelectorProvider()).start();
        ServerInfo serverInfo = ClientSearcher.searchServer(10000);
        System.out.println("Server : " + serverInfo);
        if (serverInfo != null) {
            TCPClient tcpClient = null;
            try {
                tcpClient = TCPClient.startWith(serverInfo, cachePath);
                if (tcpClient == null) {
                    return;
                }
                write(tcpClient);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (tcpClient != null) {
                    tcpClient.exit();
                }
            }
        }
        IoContext.close();
    }

    private static void write(TCPClient tcpClient) throws IOException {
        //构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));
        do {
            //键盘读取一行
            String msg = input.readLine();
            if (msg == null || Foo.COMMAND_EXIT.equalsIgnoreCase(msg)) {
                break;
            } else if (msg.length() == 0) {
                continue;
            }
            if (msg.startsWith("--f")) {
                String[] array = msg.split(" ");
                if (array.length >= 2) {
                    String filePath = array[1];
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        FileSendPacket packet = new FileSendPacket(file);
                        tcpClient.send(packet);
                        continue;
                    }
                }
            }
            //发送到服务器
            tcpClient.send(msg);
        } while (true);
    }
}
