package com.chat.room.server;

import com.chat.room.api.constants.Foo;
import com.chat.room.api.constants.TCPConstants;
import com.chat.room.api.core.IoContext;
import com.chat.room.api.core.impl.IoSelectorProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class Server {

    public static void main(String[] args) throws Exception {

        File cachePath = Foo.getCacheDir("server");
        IoContext.setup().ioProvider(new IoSelectorProvider()).start();
        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER, cachePath);
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
            if ("00bye00".equalsIgnoreCase(msg)) {
                break;
            }
            tcpServer.broadcast(msg);
        } while (true);

        UDPProvider.stop();
        tcpServer.stop();
        IoContext.close();
    }
}
