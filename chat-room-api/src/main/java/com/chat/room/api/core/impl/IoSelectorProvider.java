package com.chat.room.api.core.impl;

import com.chat.room.api.core.IoProvider;
import com.chat.room.api.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class IoSelectorProvider implements IoProvider {

    //    private final AtomicBoolean isClosed = new AtomicBoolean(false);
//
//    private final AtomicBoolean inRegInput = new AtomicBoolean(false);
//
//    private final AtomicBoolean inRegOutput = new AtomicBoolean(false);
//
//    private final Selector readSelector;
//
//    private final Selector writeSelector;
//
//    private final Map<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();
//
//    private final Map<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();
//
//    private final ExecutorService inputHandlePool;
//
//    private final ExecutorService outputHandlePool;
//
//    public IoSelectorProvider() throws IOException {
//        readSelector = Selector.open();
//        writeSelector = Selector.open();
//        inputHandlePool = Executors.newFixedThreadPool(4, new IoProviderThreadFactory("IoProvider-Input-Thread-"));
//        outputHandlePool = Executors.newFixedThreadPool(4, new IoProviderThreadFactory("IoProvider-Output-Thread-"));
//        //开始输出输入的监听
//        startRead();
//        startWrite();
//    }
//
//    @Override
//    public boolean registerInput(SocketChannel channel, HandleInputCallback callback) {
//        return registerSelection(channel, readSelector, SelectionKey.OP_READ, inRegInput, inputCallbackMap, callback) != null;
//    }
//
//    @Override
//    public boolean registerOutput(SocketChannel channel, HandleOutputCallback callback) {
//        return registerSelection(channel, writeSelector, SelectionKey.OP_WRITE, inRegOutput, outputCallbackMap, callback) != null;
//    }
//
//    @Override
//    public void unRegisterOutput(SocketChannel channel) {
//        unRegisterSelection(channel, writeSelector, outputCallbackMap);
//    }
//
//    @Override
//    public void unRegisterInput(SocketChannel channel) {
//        unRegisterSelection(channel, readSelector, inputCallbackMap);
//    }
//
//    @Override
//    public void close() {
//        if (isClosed.compareAndSet(false, true)) {
//
//            inputHandlePool.shutdown();
//            outputHandlePool.shutdown();
//
//            outputCallbackMap.clear();
//            inputCallbackMap.clear();
//
//            readSelector.wakeup();
//            writeSelector.wakeup();
//
//            CloseUtils.close(readSelector, writeSelector);
//        }
//    }
//
//    private static SelectionKey registerSelection(SocketChannel channel, Selector selector, int registerOps, AtomicBoolean locker, Map<SelectionKey, Runnable> map, Runnable runnable) {
//        synchronized (locker) {
//            locker.set(true);
//            try {
//                //唤醒当前的selector，让selector不处于select()状态
//                selector.wakeup();
//                SelectionKey key = null;
//                if (channel.isRegistered()) {
//                    //  查询是否已经注册
//                    key = channel.keyFor(selector);
//                    if (key != null) {
//                        System.out.println("interestOps registerOps : " + (key.readyOps() | registerOps));
//                        key.interestOps(key.readyOps() | registerOps);
//                    }
//                }
//                if (key == null) {
//                    //注册selector得到key
//                    System.out.println("interestOps registerOps : " + registerOps);
//                    key = channel.register(selector, registerOps);
//                    //注册回调
//                    map.put(key, runnable);
//                }
//                return key;
//            } catch (IOException e) {
//                return null;
//            } finally {
//                locker.set(false);
//                try {
//                    //通知
//                    locker.notify();
//                } catch (Exception e) {
//                }
//            }
//        }
//    }
//
//    private static void unRegisterSelection(SocketChannel channel, Selector selector, Map<SelectionKey, Runnable> map) {
//        if (channel.isRegistered()) {
//            SelectionKey key = channel.keyFor(selector);
//            if (key != null) {
//                //取消监听的方法
//                key.cancel();
//                map.remove(key);
//                selector.wakeup();
//            }
//        }
//    }
//
//    private static void handleSelection(SelectionKey key, int ops, Map<SelectionKey, Runnable> callbackMap, ExecutorService poll) {
//        //重点 取消继续对keyOps的监听
//        key.interestOps(key.readyOps() & ~ops);
//        Runnable runnable = null;
//        try {
//            runnable = callbackMap.get(key);
//        } catch (Exception e) {
//
//        }
//        if (runnable != null && !poll.isShutdown()) {
//            poll.execute(runnable);
//        }
//    }
//
//    private static void waitSelection(final AtomicBoolean locker) {
//        synchronized (locker) {
//            if (locker.get()) {
//                try {
//                    locker.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//    public void startRead() {
//        Thread thread = new Thread("Clink IoSelectorProvider ReadSelector Thread") {
//            @Override
//            public void run() {
//                super.run();
//                while (!isClosed.get()) {
//                    try {
//                        if (readSelector.select() == 0) {
//                            waitSelection(inRegInput);
//                            continue;
//                        }
//                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
//                        for (SelectionKey selectionKey : selectionKeys) {
//                            if (selectionKey.isValid()) {
//                                System.out.println("readSelector selectionKey : " + selectionKey.toString());
//                                handleSelection(selectionKey, SelectionKey.OP_READ, inputCallbackMap, inputHandlePool);
//                            }
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        };
//        thread.setPriority(Thread.MAX_PRIORITY);
//        thread.start();
//    }
//
//    public void startWrite() {
//        Thread thread = new Thread("Clink IoSelectorProvider WriteSelector Thread") {
//            @Override
//            public void run() {
//                super.run();
//                while (!isClosed.get()) {
//                    try {
//                        if (writeSelector.select() == 0) {
//                            waitSelection(inRegOutput);
//                            continue;
//                        }
//                        Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
//                        for (SelectionKey selectionKey : selectionKeys) {
//                            if (selectionKey.isValid()) {
//                                System.out.println("writeSelector selectionKey : " + selectionKey.toString());
//                                handleSelection(selectionKey, SelectionKey.OP_WRITE, outputCallbackMap, outputHandlePool);
//                            }
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        };
//        thread.setPriority(Thread.MAX_PRIORITY);
//        thread.start();
//    }
//
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


    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    // 是否处于注册Input过程当中
    private final AtomicBoolean inRegInput = new AtomicBoolean(false);
    // 是否处于注册output过程当中
    private final AtomicBoolean inRegOutput = new AtomicBoolean(false);

    private final Selector readSelector;
    private final Selector writeSelector;

    private final HashMap<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();
    private final HashMap<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();

    private final ExecutorService inputHandlePool;
    private final ExecutorService outputHandlePool;

    public IoSelectorProvider() throws IOException {
        readSelector = Selector.open();
        writeSelector = Selector.open();

        inputHandlePool = Executors.newFixedThreadPool(20,
                // namePrefix线程前缀
                new IoProviderThreadFactory("IoProvider-Input-Thread-"));
        outputHandlePool = Executors.newFixedThreadPool(20, new IoProviderThreadFactory("IoProvider-Output-Thread-"));

        // 开始输出输入的监听
        startRead();
        startWrite();
    }

    public boolean registerInput(SocketChannel channel, HandleProviderCallback callback) {
        return registerSelection(channel, readSelector, SelectionKey.OP_READ, inRegInput, inputCallbackMap,
                callback) != null;
    }

    public boolean registerOutput(SocketChannel channel, HandleProviderCallback callback) {
        return registerSelection(channel, writeSelector, SelectionKey.OP_WRITE, inRegOutput, outputCallbackMap,
                callback) != null;
    }

    public void unRegisterInput(SocketChannel channel) {
        unRegisterSelection(channel, readSelector, inputCallbackMap, inRegInput);
    }

    public void unRegisterOutput(SocketChannel channel) {
        unRegisterSelection(channel, writeSelector, outputCallbackMap, inRegOutput);
    }

    private static void unRegisterSelection(SocketChannel channel, Selector selector, Map<SelectionKey, Runnable> map, AtomicBoolean locker) {
        synchronized (locker) {
            locker.set(true);
            selector.wakeup();
            try {
                if (channel.isRegistered()) {
                    SelectionKey key = channel.keyFor(selector);
                    if (key != null) {
                        // 取消监听的方法
                        key.channel();
                        map.remove(key);
                    }
                }
            } finally {
                locker.set(false);
                try {
                    locker.notifyAll();
                } catch (Exception e) {
                }
            }
        }
    }

    private static void handleSelection(SelectionKey key, int keyOps, HashMap<SelectionKey, Runnable> map,
                                        ExecutorService pool, AtomicBoolean locker) {
        synchronized (locker) {
            try {
                // 重点
                // 取消继续对keyOps的监听
                key.interestOps(key.readyOps() & ~keyOps);
            } catch (CancelledKeyException e) {
                return;
            }
        }
        Runnable runnable = null;
        try {
            runnable = map.get(key);
        } catch (Exception e) {

        }
        if (runnable != null && !pool.isShutdown()) {
            // 异步调度
            pool.execute(runnable);
        }
    }

    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            inputHandlePool.shutdown();
            outputHandlePool.shutdown();
            inputCallbackMap.clear();
            outputCallbackMap.clear();
            CloseUtils.close(readSelector, writeSelector);
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

    private static SelectionKey registerSelection(SocketChannel channel, Selector selector, int registerOps,
                                                  AtomicBoolean locker, Map<SelectionKey, Runnable> map, Runnable runnable) {
        synchronized (locker) {
            // 设置锁定状态
            locker.set(true);
            try {
                // 唤醒当前的selector，让selector不处于select()状态
                // 因为我们处于一个select状态的时候是一个阻塞状态，就是我们当前的selector里面注册的东西变更了，但是
                // selector没有进行二次select时，有可能会出现当前这个SocketChannel他的注册是无效的。
                selector.wakeup();
                SelectionKey key = null;
                if (channel.isRegistered()) {
                    // 查询是否已经注册过
                    key = channel.keyFor(selector);
                    if (key != null) {
                        // 把新的需要注册的状态注册进去
                        key.interestOps(key.readyOps() | registerOps);
                    }
                }
                if (key == null) {
                    // 注册selector得到key
                    key = channel.register(selector, registerOps);
                    // 注册回调
                    map.put(key, runnable);
                }
                return key;
            } catch (ClosedChannelException | CancelledKeyException | ClosedSelectorException e) {
                return null;
            } finally {
                // 解除锁定状态
                locker.set(false);
                try {
                    // 通知
                    locker.notify();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startRead() {
        Thread thread = new SelectThread("Clink IoSelectorProvider ReadSelector Thread", isClosed, inRegInput, readSelector, inputCallbackMap, inputHandlePool, SelectionKey.OP_READ);
        thread.start();
    }

    private void startWrite() {
        Thread thread = new SelectThread("Clink IoSelectorProvider WriteSelector Thread", isClosed, inRegOutput, writeSelector, outputCallbackMap, outputHandlePool, SelectionKey.OP_WRITE);
        thread.start();
    }

    static class SelectThread extends Thread {
        private final AtomicBoolean isClosed;
        private final AtomicBoolean locker;
        private final Selector selector;
        private final HashMap<SelectionKey, Runnable> callMap;
        private final ExecutorService pool;
        private final int keyOps;

        SelectThread(String name, AtomicBoolean isClosed, AtomicBoolean locker, Selector selector, HashMap<SelectionKey, Runnable> callMap, ExecutorService pool, int keyOps) {
            super(name);
            this.locker = locker;
            this.isClosed = isClosed;
            this.selector = selector;
            this.callMap = callMap;
            this.pool = pool;
            this.keyOps = keyOps;
            this.setPriority(Thread.MAX_PRIORITY);
        }

        @Override
        public void run() {
            AtomicBoolean locker = this.locker;
            AtomicBoolean isClosed = this.isClosed;
            Selector selector = this.selector;
            HashMap<SelectionKey, Runnable> callMap = this.callMap;
            ExecutorService pool = this.pool;
            int keyOps = this.keyOps;
            while (!isClosed.get()) {
                try {
                    if (selector.select() == 0) {
                        waitSelection(locker);
                        continue;
                    } else if (locker.get()) {
                        waitSelection(locker);
                    }
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey selectionKey = iterator.next();
                        if (selectionKey.isValid()) {
                            handleSelection(selectionKey, keyOps, callMap, pool, locker);
                        }
                        iterator.remove();
                    }
                    selectionKeys.clear();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClosedSelectorException e) {
                    break;
                }
            }
        }
    }

}
