package com.chat.room.server;

import com.chat.room.api.constants.TCPConstants;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Server {

    public static void main(String[] args) throws Exception {

        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER);
        boolean isSucceed = tcpServer.start();
        if (!isSucceed) {
            System.out.println("Start TCP server failed!");
            return;
        }

        UDPProvider.start(TCPConstants.PORT_SERVER);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String msg;
        do {
            msg = bufferedReader.readLine();
            if (msg == null) {
                continue;
            }
            tcpServer.broadcast(msg);
        } while (!"00bye00".equalsIgnoreCase(msg));

        UDPProvider.stop();
        tcpServer.stop();
    }
}
