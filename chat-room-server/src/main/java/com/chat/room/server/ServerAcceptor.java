package com.chat.room.server;

import com.chat.room.api.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

public class ServerAcceptor extends Thread {

    private final AcceptListener listener;
    private final Selector selector;
    private final CountDownLatch latch = new CountDownLatch(1);
    private boolean done = false;

    ServerAcceptor(AcceptListener listener) throws IOException {
        super("Server-Accept-Thread");
        this.listener = listener;
        selector = Selector.open();
    }

    boolean awaitRunning() {
        try {
            latch.await();
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public void run() {
        super.run();
        System.out.println("latch.countDown()");
        latch.countDown();
        Selector selector = this.selector;
        do {
            //得到客户端
            try {
                if (selector.select() == 0) {
                    if (done) {
                        break;
                    }
                    continue;
                }
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    if (done) {
                        break;
                    }
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    //检查当前key的状态是否是我们关注的
                    if (key.isAcceptable()) {
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                        //非阻塞状态拿到客户端连接
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        listener.onNewSocketArrived(socketChannel);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (!done);
        System.out.println("ServerAcceptor Finished!");
    }

    void exit() {
        done = true;
        CloseUtils.close(selector);
    }

    public Selector getSelector() {
        return selector;
    }

    public interface AcceptListener {

        void onNewSocketArrived(SocketChannel channel);

    }

}
