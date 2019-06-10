package com.chat.room.client;

import com.chat.room.api.bean.ServerInfo;
import com.chat.room.api.constants.Foo;
import com.chat.room.api.core.IoContext;
import com.chat.room.api.core.impl.IoSelectorProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientTest {

    private static boolean done;

    public static void main(String[] args) throws IOException {

        File cachePath = Foo.getCacheDir("client/test");
        IoContext.setup().ioProvider(new IoSelectorProvider()).start();
        ServerInfo serverInfo = ClientSearcher.searchServer(10000);
        System.out.println("Server : " + serverInfo);

        if (serverInfo == null) {
            return;
        }

        int size = 0;
        List<TCPClient> clients = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            try {
                TCPClient tcpClient = TCPClient.startWith(serverInfo, cachePath);
                if (tcpClient == null) {
                    throw new NullPointerException();
                }
                clients.add(tcpClient);
                System.out.println("连接成功" + (++size));
            } catch (IOException | NullPointerException e) {
                System.out.println("连接异常");
                break;
            }
        }

        System.in.read();

        Runnable runnable = () -> {
            while (!done) {
                for (TCPClient client : clients) {
                    client.send("Hello!!");
                }
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();

        System.in.read();
        //等待线程完成
        done = true;

        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //客户端操作完成
        for (TCPClient client : clients) {
            client.exit();
        }

        IoContext.close();
    }
}
