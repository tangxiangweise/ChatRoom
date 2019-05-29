package com.chat.room.client;

import com.chat.room.api.bean.ServerInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientTest {

    private static boolean done;

    public static void main(String[] args) throws IOException {

        ServerInfo serverInfo = ClientSearcher.searchServer(10000);
        System.out.println("Server : " + serverInfo);

        if (serverInfo == null) {
            return;
        }

        int size = 0;
        List<TCPClient> clients = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            try {
                TCPClient tcpClient = TCPClient.startWith(serverInfo);
                if (tcpClient == null) {
                    System.out.println("连接异常");
                    continue;
                }
                clients.add(tcpClient);
                System.out.println("连接成功" + (++size));
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("连接异常");
            }
            try {
                Thread.sleep(20);
            } catch (Exception e) {

            }
        }

        System.in.read();

        Runnable runnable = () -> {
            while (!done) {
                for (TCPClient client : clients) {
                    client.send("Hello!!");
                }
                try {
                    Thread.sleep(1000);
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
    }
}
