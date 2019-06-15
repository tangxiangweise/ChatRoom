package com.chat.room.api.core.impl;

import com.chat.room.api.core.IoProvider;
import com.chat.room.api.core.impl.stealing.IoTask;
import com.chat.room.api.core.impl.stealing.StealingSelectorThread;
import com.chat.room.api.core.impl.stealing.StealingService;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * 可窃取任务的IoProvider
 */
public class IoStealingSelectorProvider implements IoProvider {

    private final IoStealingThread[] threads;
    private final StealingService stealingService;

    public IoStealingSelectorProvider(int poolSize) throws IOException {
        IoStealingThread[] threads = new IoStealingThread[poolSize];
        for (int i = 0; i < poolSize; i++) {
            Selector selector = Selector.open();
            threads[i] = new IoStealingThread("IoProvider-Thread-" + (i + 1), selector);
        }

        StealingService stealingService = new StealingService(threads, 10);
        for (IoStealingThread thread : threads) {
            thread.setStealingService(stealingService);
            thread.start();
        }
        this.threads = threads;
        this.stealingService = stealingService;
    }

    @Override
    public boolean registerInput(SocketChannel channel, HandleProviderCallback callback) {
        StealingSelectorThread thread = stealingService.getNotBusyThread();
        if (thread != null) {
            return thread.register(channel, SelectionKey.OP_READ, callback);
        }
        return false;
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleProviderCallback callback) {
        StealingSelectorThread thread = stealingService.getNotBusyThread();
        if (thread != null) {
            return thread.register(channel, SelectionKey.OP_WRITE, callback);
        }
        return false;
    }

    @Override
    public void unRegisterInput(SocketChannel channel) {
        for (IoStealingThread thread : threads) {
            thread.unRegister(channel);
        }
    }

    @Override
    public void unRegisterOutput(SocketChannel channel) {
    }

    @Override
    public void close() {
        stealingService.shutdown();
    }

    private class IoStealingThread extends StealingSelectorThread {

        public IoStealingThread(String name, Selector selector) {
            super(selector);
            setName(name);
        }

        @Override
        protected boolean processTask(IoTask task) {
            stealingService.execute(task);
//            task.providerCallback.run();
            return false;
        }

    }

}
