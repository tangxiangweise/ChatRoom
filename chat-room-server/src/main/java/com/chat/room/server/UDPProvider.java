package com.chat.room.server;

import com.chat.room.api.constants.UDPConstants;
import com.chat.room.api.utils.ByteUtils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

public class UDPProvider {

    private static Provider PROVIDER_INSTANCE;

    public static void start(int port) {
        stop();
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn, port);
        provider.start();
        PROVIDER_INSTANCE = provider;

    }

    public static void stop() {
        if (PROVIDER_INSTANCE != null) {
            PROVIDER_INSTANCE.exit();
            PROVIDER_INSTANCE = null;
        }
    }

    private static class Provider extends Thread {

        private final byte[] sn;
        private final int port;
        private boolean done = false;
        private DatagramSocket ds = null;
        //存储消息的bytes
        final byte[] bytes = new byte[128];

        public Provider(String sn, int port) {
            super("Server-UDPProvider-Thread");
            this.sn = sn.getBytes();
            this.port = port;
        }

        @Override
        public void run() {
            super.run();
            System.out.println("UDPProvider Started");
            try {
                //监听20000端口
                ds = new DatagramSocket(UDPConstants.PORT_SERVER);
                //接收消息Packet
                DatagramPacket receivePack = new DatagramPacket(bytes, bytes.length);
                while (!done) {
                    ds.receive(receivePack);

                    String clientIp = receivePack.getAddress().getHostAddress();
                    int clientPort = receivePack.getPort();
                    int clientDataLen = receivePack.getLength();
                    byte[] clientData = receivePack.getData();
                    boolean isValid = clientDataLen >= (UDPConstants.HEADER.length + 2 + 4) && ByteUtils.startsWith(clientData, UDPConstants.HEADER);
                    System.out.println("ServerProvider receive form ip : " + clientIp + " port : " + clientPort + " dataValid : " + isValid);
                    if (!isValid) {
                        continue;
                    }
                    //解析命令与回送端口
                    int index = UDPConstants.HEADER.length;
                    short cmd = (short) ((clientData[index++] << 8) | (clientData[index++] & 0xff));
                    short responsePort = (short) (((clientData[index++]) << 24) | ((clientData[index++] & 0xff) << 16) | ((clientData[index++] & 0xff) << 8) | (clientData[index++] & 0xff));
                    //校验合法性
                    if (cmd == 1 && responsePort > 0) {
                        //回送服务器信息
                        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
                        byteBuffer.put(UDPConstants.HEADER);
                        byteBuffer.putShort((short) 2);
                        byteBuffer.putInt(port);
                        byteBuffer.put(sn);
                        int len = byteBuffer.position();
                        DatagramPacket responsePacket = new DatagramPacket(bytes, len, receivePack.getAddress(), responsePort);
                        ds.send(responsePacket);
                        System.out.println("ServerProvider response to :" + clientIp + " port : " + responsePort + " dataLen :" + len);
                    } else {
                        System.out.println("ServerProvider receive cmd nonsupport; cmd " + cmd + " port : " + port);
                    }
                }
            } catch (Exception e) {

            } finally {
                close();
            }
            System.out.println("UDPProvider Finished");
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        public void exit() {
            done = true;
            close();
        }
    }
}
