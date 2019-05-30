package com.chat.room.api.core.impl;

import com.chat.room.api.core.IoProvider;
import com.chat.room.api.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class IoSelectorProvider implements IoProvider {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final AtomicBoolean inRegInput = new AtomicBoolean(false);

    private final AtomicBoolean inRegOutput = new AtomicBoolean(false);

    private final Selector readSelector;

    private final Selector writeSelector;

    private final Map<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();

    private final Map<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();

    private final ExecutorService inputHandlePool;

    private final ExecutorService outputHandlePool;

    public IoSelectorProvider() throws IOException {
        readSelector = Selector.open();
        writeSelector = Selector.open();
        inputHandlePool = Executors.newFixedThreadPool(4, new IoProviderThreadFactory("IoProvider-Input-Thread-"));
        outputHandlePool = Executors.newFixedThreadPool(4, new IoProviderThreadFactory("IoProvider-Output-Thread-"));
        //开始输出输入的监听
        startRead();
        startWrite();
    }

    @Override
    public boolean registerInput(SocketChannel channel, HandleInputCallback callback) {
        return registerSelection(channel, readSelector, SelectionKey.OP_READ, inRegInput, inputCallbackMap, callback) != null;
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleOutputCallback callback) {
        return registerSelection(channel, writeSelector, SelectionKey.OP_WRITE, inRegOutput, outputCallbackMap, callback) != null;
    }

    @Override
    public void unRegisterOutput(SocketChannel channel) {
        unRegisterSelection(channel, writeSelector, outputCallbackMap);
    }

    @Override
    public void unRegisterInput(SocketChannel channel) {
        unRegisterSelection(channel, readSelector, inputCallbackMap);
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {

            inputHandlePool.shutdown();
            outputHandlePool.shutdown();

            outputCallbackMap.clear();
            inputCallbackMap.clear();

            readSelector.wakeup();
            writeSelector.wakeup();

            CloseUtils.close(readSelector, writeSelector);
        }
    }

    private static SelectionKey registerSelection(SocketChannel channel, Selector selector, int registerOps, AtomicBoolean locker, Map<SelectionKey, Runnable> map, Runnable runnable) {
        synchronized (locker) {
            locker.set(true);
            try {
                //唤醒当前的selector，让selector不处于select()状态
                selector.wakeup();
                SelectionKey key = null;
                if (channel.isRegistered()) {
                    //  查询是否已经注册
                    key = channel.keyFor(selector);
                    if (key != null) {
                        key.interestOps(key.readyOps() | registerOps);
                    }
                }
                if (key == null) {
                    //注册selector得到key
                    key = channel.register(selector, registerOps);
                    //注册回调
                    map.put(key, runnable);
                }
                return key;
            } catch (IOException e) {
                return null;
            } finally {
                locker.set(false);
                try {
                    //通知
                    locker.notify();
                } catch (Exception e) {
                }
            }
        }
    }

    private static void unRegisterSelection(SocketChannel channel, Selector selector, Map<SelectionKey, Runnable> map) {
        if (channel.isRegistered()) {
            SelectionKey key = channel.keyFor(selector);
            if (key != null) {
                //取消监听的方法
                key.cancel();
                map.remove(key);
                selector.wakeup();
            }
        }
    }

    private static void handleSelection(SelectionKey key, int ops, Map<SelectionKey, Runnable> callbackMap, ExecutorService poll) {
        //重点 取消继续对keyOps的监听
        key.interestOps(key.readyOps() & ~ops);
        Runnable runnable = null;
        try {
            runnable = callbackMap.get(key);
        } catch (Exception e) {

        }
        if (runnable != null && !poll.isShutdown()) {
            poll.execute(runnable);
        }
    }

    private static void waitSelection(final AtomicBoolean locker) {
        synchronized (locker) {
            if (locker.get()) {
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void startRead() {
        Thread thread = new Thread("Clink IoSelectorProvider ReadSelector Thread") {
            @Override
            public void run() {
                super.run();
                while (!isClosed.get()) {
                    try {
                        if (readSelector.select() == 0) {
                            waitSelection(inRegInput);
                            continue;
                        }
                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            if (selectionKey.isValid()) {
                                System.out.println("selectionKey : " + selectionKey.toString());
                                handleSelection(selectionKey, SelectionKey.OP_READ, inputCallbackMap, inputHandlePool);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    public void startWrite() {
        Thread thread = new Thread("Clink IoSelectorProvider WriteSelector Thread") {
            @Override
            public void run() {
                super.run();
                while (!isClosed.get()) {
                    try {
                        if (writeSelector.select() == 0) {
                            waitSelection(inRegOutput);
                            continue;
                        }
                        Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            if (selectionKey.isValid()) {
                                handleSelection(selectionKey, SelectionKey.OP_WRITE, outputCallbackMap, outputHandlePool);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    static class IoProviderThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        IoProviderThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

}
