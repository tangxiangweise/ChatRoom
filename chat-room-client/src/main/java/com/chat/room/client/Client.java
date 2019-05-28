package com.chat.room.client;

import com.chat.room.api.bean.ServerInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Client {

    public static void main(String[] args) {
        ServerInfo serverInfo = ClientSearcher.searchServer(10000);
        System.out.println("Server : " + serverInfo);
        if (serverInfo != null) {
            TCPClient tcpClient = null;
            try {
                tcpClient = TCPClient.startWith(serverInfo);
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
    }

    private static void write(TCPClient tcpClient) throws IOException {
        //构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));
        do {
            //键盘读取一行
            String msg = input.readLine();
            //发送到服务器
            tcpClient.send(msg);
            if ("00bye00".equalsIgnoreCase(msg)) {
                break;
            }
        } while (true);
    }
}
