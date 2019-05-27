package com.chat.room.client;

import com.chat.room.api.bean.ServerInfo;

public class Client {

    public static void main(String[] args) {
        ServerInfo serverInfo = ClientSearcher.searchServer(10000);
        System.out.println("Server : " + serverInfo);
        if (serverInfo != null) {
            try {
                TCPClient.linkWith(serverInfo);
            } catch (Exception e) {

            }
        }
    }

}
