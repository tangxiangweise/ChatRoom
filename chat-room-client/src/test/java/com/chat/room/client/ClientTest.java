package com.chat.room.client;

import com.chat.room.api.bean.ServerInfo;
import com.chat.room.api.constants.Foo;
import com.chat.room.api.core.Connector;
import com.chat.room.api.core.IoContext;
import com.chat.room.api.core.impl.IoStealingSelectorProvider;
import com.chat.room.api.core.impl.SchedulerImpl;
import com.chat.room.api.handler.ConnectorCloseChain;
import com.chat.room.api.handler.ConnectorHandler;
import com.chat.room.api.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientTest {

    //不考虑发送消耗，并发量：2000 * 4/ 400*1000 =2w/s 算上来回两次解析 4w/s
    private static final int CLIENT_SIZE = 2000;
    private static final int SEND_THREAD_SIZE = 4;
    private static final int SEND_THREAD_DELAY = 200;
    private static volatile boolean done;


    public static void main(String[] args) throws IOException {

        ServerInfo serverInfo = ClientSearcher.searchServer(10000);
        System.out.println("Server : " + serverInfo);
        if (serverInfo == null) {
            return;
        }

        File cachePath = Foo.getCacheDir("client/test");
        IoContext.setup().ioProvider(new IoStealingSelectorProvider(3)).scheduler(new SchedulerImpl(1)).start();
        //当前连接数量
        int size = 0;
        List<TCPClient> clients = new ArrayList<>(CLIENT_SIZE);

        final ConnectorCloseChain closeChain = new ConnectorCloseChain() {
            @Override
            protected boolean consume(ConnectorHandler handler, Connector connector) {
                clients.remove(handler);
                if (clients.size() == 0) {
                    CloseUtils.close(System.in);
                }
                return false;
            }
        };


        for (int i = 0; i < CLIENT_SIZE; i++) {
            try {
                TCPClient tcpClient = TCPClient.startWith(serverInfo, cachePath, false);
                if (tcpClient == null) {
                    throw new NullPointerException();
                }
                //添加关闭链式节点
                tcpClient.getCloseChain().appendLast(closeChain);
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
                TCPClient[] copyClients = clients.toArray(new TCPClient[0]);
                for (TCPClient client : copyClients) {
                    client.send("Hello!!");
                }
                if (SEND_THREAD_DELAY > 0) {
                    try {
                        Thread.sleep(SEND_THREAD_DELAY);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        List<Thread> threads = new ArrayList<>(SEND_THREAD_SIZE);
        for (int i = 0; i < SEND_THREAD_SIZE; i++) {
            Thread thread = new Thread(runnable);
            thread.start();
            threads.add(thread);
        }

        System.in.read();
        //等待线程完成
        done = true;
        //客户端操作完成
        for (TCPClient client : clients) {
            client.exit();
        }

        IoContext.close();

        for (Thread thread : threads) {
            try {
                thread.interrupt();
            } catch (Exception e) {
            }
        }

    }
}
