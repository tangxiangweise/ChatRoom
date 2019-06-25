package com.chat.room.api.core.impl.stealing;

import com.chat.room.api.core.IoProvider;
import com.chat.room.api.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 可窃取任务的线程
 */
public abstract class StealingSelectorThread extends Thread {

    private final Selector selector;
    //是否还处于运行中
    private volatile boolean isRunning = true;
    //允许的操作
    private static final int VALID_OPS = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    // 已就绪任务队列
    private final LinkedBlockingQueue<IoTask> readyTaskQueue = new LinkedBlockingQueue<>();
    // 待注册的任务队列
    private final LinkedBlockingQueue<IoTask> registerTaskQueue = new LinkedBlockingQueue<>();
    // 单次就绪的任务缓存，随后一次性加入到就绪队列中
    private final List<IoTask> onceReadyTaskCache = new ArrayList<>(200);
    // 任务饱和度度量
    private final AtomicLong saturatingCapacity = new AtomicLong();
    // 用于多线程协同的Service
    private volatile StealingService stealingService;


    public StealingSelectorThread(Selector selector) {
        this.selector = selector;
    }

    /**
     * 绑定 StealingService
     *
     * @param stealingService
     */
    public void setStealingService(StealingService stealingService) {
        this.stealingService = stealingService;
    }

    /**
     * 将通道注册到当前的selector中
     *
     * @param channel  通道
     * @param ops      关注的行为
     * @param callback 触发时的回调
     * @return 是否注册成功
     */
    public boolean register(SocketChannel channel, int ops, IoProvider.HandleProviderCallback callback) {
        if (channel.isOpen()) {
            IoTask task = new IoTask(channel, ops, callback);
            registerTaskQueue.offer(task);
            return true;
        }
        return false;
    }

    /**
     * 取消注册，原理类似于注册操作在队列中添加一份取消注册的任务，并将副本变量清空
     *
     * @param channel 通道
     */
    public void unRegister(SocketChannel channel) {
        SelectionKey selectionKey = channel.keyFor(selector);
        if (selectionKey != null && selectionKey.attachment() != null) {
            // 关闭前可使用attach简单判断是否已处于队列中
            selectionKey.attach(null);
            // 添加取消操作
            IoTask task = new IoTask(channel, 0, null);
            registerTaskQueue.offer(task);
        }
    }

    /**
     * 获取内部的任务队列
     *
     * @return 任务队列
     */
    public LinkedBlockingQueue<IoTask> getReadyTaskQueue() {
        return readyTaskQueue;
    }

    /**
     * 获取饱和程度
     * 暂时的饱和度量是使用任务执行次数来定
     *
     * @return -1 已失效
     */
    public long getSaturatingCapacity() {
        if (selector.isOpen()) {
            return saturatingCapacity.get();
        }
        return -1;
    }

    /**
     * 消费当前待注册的通道任务
     *
     * @param registerTaskQueue 待注册的通道
     */
    private void consumeRegisterTodoTasks(final LinkedBlockingQueue<IoTask> registerTaskQueue) {
        final Selector selector = this.selector;

        IoTask registerTask = registerTaskQueue.poll();
        while (registerTask != null) {
            try {
                final SocketChannel channel = registerTask.channel;
                int ops = registerTask.ops;
                if (ops == 0) {
                    // 取消操作
                    SelectionKey key = channel.keyFor(selector);
                    if (key != null) {
                        key.cancel();
                    }
                } else if ((ops & ~VALID_OPS) == 0) {
                    SelectionKey key = channel.keyFor(selector);
                    if (key == null) {
                        key = channel.register(selector, ops, new KeyAttachment());
                    } else {
                        // 已注册不需要重复注册 添加新的关注事件
                        key.interestOps(key.interestOps() | ops);
                    }

                    Object attachment = key.attachment();
                    if (attachment instanceof KeyAttachment) {
                        ((KeyAttachment) attachment).attach(ops, registerTask);
                    } else {
                        // 外部关闭，直接取消
                        key.cancel();
                    }
                }
            } catch (ClosedChannelException | CancelledKeyException | ClosedSelectorException e) {
            } finally {
                registerTask = registerTaskQueue.poll();
            }
        }
    }

    /**
     * 将单次就绪的任务缓存加入到总队列中
     *
     * @param readyTaskQueue     总任务队列
     * @param onceReadyTaskCache 单次待执行的任务
     */
    private void joinTaskQueue(final LinkedBlockingQueue<IoTask> readyTaskQueue, final List<IoTask> onceReadyTaskCache) {
        readyTaskQueue.addAll(onceReadyTaskCache);
    }

    /**
     * 消费待完成的任务
     *
     * @param readyTaskQueue
     * @param registerTaskQueue
     */
    private void consumeTodoTasks(final LinkedBlockingQueue<IoTask> readyTaskQueue, LinkedBlockingQueue<IoTask> registerTaskQueue) {
        final AtomicLong saturatingCapacity = this.saturatingCapacity;
        // 循环把所有任务做完
        IoTask doTask = readyTaskQueue.poll();
        while (doTask != null) {
            //增加饱和度
            saturatingCapacity.incrementAndGet();
            //做任务
            if (processTask(doTask)) {
                // 做完工作后添加待注册的列表
                registerTaskQueue.offer(doTask);
            }
            //下个任务
            doTask = readyTaskQueue.poll();
        }
        //窃取其它任务
        final StealingService stealingService = this.stealingService;
        if (stealingService != null) {
            doTask = stealingService.steal(readyTaskQueue);
            while (doTask != null) {
                saturatingCapacity.incrementAndGet();
                if (processTask(doTask)) {
                    registerTaskQueue.offer(doTask);
                }
                doTask = stealingService.steal(readyTaskQueue);
            }
        }
    }

    @Override
    public void run() {
        super.run();

        final Selector selector = this.selector;
        final LinkedBlockingQueue<IoTask> readyTaskQueue = this.readyTaskQueue;
        final LinkedBlockingQueue<IoTask> registerTaskQueue = this.registerTaskQueue;
        final List<IoTask> onceReadyTaskCache = this.onceReadyTaskCache;

        try {
            while (isRunning) {
                // 加入待注册的通道
                consumeRegisterTodoTasks(registerTaskQueue);
                // 检查一次
                if ((selector.selectNow()) == 0) {
                    Thread.yield();
                    continue;
                }
                // 处理已就绪的通道
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                //迭代已就绪的任务
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    Object attachmentObj = selectionKey.attachment();
                    //检查有效性
                    if (selectionKey.isValid() && attachmentObj instanceof KeyAttachment) {
                        final KeyAttachment attachment = (KeyAttachment) attachmentObj;
                        try {
                            int interestOps = selectionKey.interestOps();
                            // 是否可读
                            if (selectionKey.isReadable()) {
                                onceReadyTaskCache.add(attachment.taskForReadable);
                                interestOps = interestOps & ~SelectionKey.OP_READ;
                            }
                            // 是否可写
                            if (selectionKey.isWritable()) {
                                onceReadyTaskCache.add(attachment.taskForWritable);
                                interestOps = interestOps & ~SelectionKey.OP_WRITE;
                            }
                            // 取消已就绪的关注事件 读写都触发不关注任何事情
                            selectionKey.interestOps(interestOps);
                        } catch (CancelledKeyException e) {
                            //当前连接被取消、断开时直接移除相关任务
                            onceReadyTaskCache.remove(attachment.taskForReadable);
                            onceReadyTaskCache.remove(attachment.taskForWritable);
                        }
                    }
                    iterator.remove();
                }
                //判断本次是否有待执行的任务
                if (!onceReadyTaskCache.isEmpty()) {
                    //加入到总队列中
                    joinTaskQueue(readyTaskQueue, onceReadyTaskCache);
                    onceReadyTaskCache.clear();
                }
                //消费总队列中的任务
                consumeTodoTasks(readyTaskQueue, registerTaskQueue);
            }
        } catch (ClosedSelectorException e) {
        } catch (IOException e) {
            CloseUtils.close(selector);
        } finally {
            readyTaskQueue.clear();
            registerTaskQueue.clear();
            onceReadyTaskCache.clear();
        }
    }

    public void exit() {
        isRunning = false;
        CloseUtils.close(selector);
        interrupt();
    }

    /**
     * 调用子类执行任务操作
     *
     * @param task 任务
     * @return 执行任务后是否需要再次添加该任务
     */
    protected abstract boolean processTask(IoTask task);

    /**
     * 用以注册时添加的附件
     */
    static class KeyAttachment {
        //可读取时执行的任务
        IoTask taskForReadable;
        //可写时执行的任务
        IoTask taskForWritable;

        /**
         * 附加任务
         *
         * @param ops  任务关注
         * @param task
         */
        void attach(int ops, IoTask task) {
            if (ops == SelectionKey.OP_READ) {
                taskForReadable = task;
            } else {
                taskForWritable = task;
            }
        }

    }

}
