package com.chat.room.client;

import com.chat.room.api.bean.ServerInfo;
import com.chat.room.api.constants.UDPConstants;
import com.chat.room.api.utils.ByteUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClientSearcher {

    private static final int LISTEN_PORT = UDPConstants.PORT_CLIENT_RESPONSE;

    public static ServerInfo searchServer(int timeout) {
        System.out.println("UDPSearcher Started.");
        CountDownLatch receiveLatch = new CountDownLatch(1);
        Listener listener = null;
        try {
            listener = listen(receiveLatch);
            sendBroadcast();
            receiveLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("UDPSearcher Finished.");
        if (listener == null) {
            return null;
        }
        List<ServerInfo> servers = listener.getServersAndClose();
        if (servers.size() > 0) {
            return servers.get(0);
        }
        return null;
    }

    private static Listener listen(CountDownLatch receiveLatch) throws Exception {
        System.out.println("UDPSearcher start listen");
        CountDownLatch startDownLatch = new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_PORT, startDownLatch, receiveLatch);
        listener.start();
        startDownLatch.await();
        return listener;
    }

    private static void sendBroadcast() throws IOException {
        System.out.println("UDPSearcher sendBroadcast started.");

        DatagramSocket ds = new DatagramSocket();
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        byteBuffer.put(UDPConstants.HEADER);
        byteBuffer.putShort((short) 1);
        byteBuffer.putInt(LISTEN_PORT);

        DatagramPacket requestPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.position() + 1);
        requestPacket.setAddress(InetAddress.getByName("255.255.255.255"));
        requestPacket.setPort(UDPConstants.PORT_SERVER);

        ds.send(requestPacket);
        ds.close();
    }

    private static class Listener extends Thread {
        private final int listenPort;
        private final CountDownLatch startDownLatch;
        private final CountDownLatch receiveDownLatch;
        private final List<ServerInfo> servers = new ArrayList<>();
        private final byte[] buffer = new byte[128];
        private final int minLen = UDPConstants.HEADER.length + 2 + 4;
        private boolean done = false;
        private DatagramSocket ds = null;

        public Listener(int listenPort, CountDownLatch startDownLatch, CountDownLatch receiveDownLatch) {
            super();
            this.listenPort = listenPort;
            this.startDownLatch = startDownLatch;
            this.receiveDownLatch = receiveDownLatch;
        }

        @Override
        public void run() {
            super.run();
            System.out.println("UDPSearcher Listener Started");
            startDownLatch.countDown();
            try {
                ds = new DatagramSocket(listenPort);
                //构建接收实体
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                while (!done) {
                    //接收
                    ds.receive(receivePacket);

                    //打印接受者的信息于发送者的信息
                    String ip = receivePacket.getAddress().getHostAddress();
                    int port = receivePacket.getPort();
                    int dataLen = receivePacket.getLength();
                    byte[] data = receivePacket.getData();

                    boolean isValid = dataLen >= minLen && ByteUtils.startsWith(data, UDPConstants.HEADER);
                    System.out.println("UDPSearcher receive form ip : " + ip + " port : " + port + " dataValid : " + isValid);
                    if (!isValid) {
                        continue;
                    }
                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, UDPConstants.HEADER.length, dataLen);
                    final short cmd = byteBuffer.getShort();
                    final int serverPort = byteBuffer.getInt();
                    if (cmd != 2 || serverPort <= 0) {
                        System.out.println("UDPSearcher receive cmd : " + cmd + " serverPort : " + serverPort);
                        continue;
                    }
                    String sn = new String(buffer, minLen, dataLen - minLen);
                    ServerInfo serverInfo = new ServerInfo(sn, serverPort, ip);
                    servers.add(serverInfo);
                    //成功收到一份
                    receiveDownLatch.countDown();
                }
            } catch (Exception e) {

            } finally {
                close();
            }
            System.out.println("UDPSearcher Listener Finished.");
        }

        public List<ServerInfo> getServersAndClose() {
            done = true;
            close();
            return servers;
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }


    }
}
