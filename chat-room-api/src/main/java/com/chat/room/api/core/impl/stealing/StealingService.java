package com.chat.room.api.core.impl.stealing;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntFunction;

public class StealingService {

    /**
     * 当任务队列数量低于安全值时，不可窃取
     */
    private final int minSafetyThreshold;
    /**
     * 线程集合
     */
    private StealingSelectorThread[] threads;
    /**
     * 对应的任务队列
     */
    private final LinkedBlockingQueue<IoTask>[] queues;
    /**
     * 结束标记
     */
    private volatile boolean isTerminate;


    public StealingService(StealingSelectorThread[] threads, int minSafetyThreshold) {
        this.minSafetyThreshold = minSafetyThreshold;
        this.threads = threads;
        this.queues = Arrays.stream(threads).map(StealingSelectorThread::getReadyTaskQueue).toArray((IntFunction<LinkedBlockingQueue<IoTask>[]>) LinkedBlockingQueue[]::new);
    }

    /**
     * 窃取一个任务，排除自己，从其它队列取一个任务
     *
     * @param excludedQueue 待排除队列
     * @return 窃取成功返回实例
     */
    IoTask steal(final LinkedBlockingQueue<IoTask> excludedQueue) {
        final int minSafetyThreshold = this.minSafetyThreshold;
        LinkedBlockingQueue<IoTask>[] queues = this.queues;
        for (LinkedBlockingQueue<IoTask> queue : queues) {
            if (queue == excludedQueue) {
                continue;
            }
            int size = queue.size();
            if (size > minSafetyThreshold) {
                IoTask task = queue.poll();
                if (task != null) {
                    return task;
                }
            }
        }
        return null;
    }

    /**
     * 获取一个不繁忙的线程
     *
     * @return
     */
    public StealingSelectorThread getNotBusyThread() {
        StealingSelectorThread targetThread = null;
        long targetKeyCount = Long.MAX_VALUE;
        for (StealingSelectorThread thread : threads) {
            long saturatingCapacity = thread.getSaturatingCapacity();
            if (saturatingCapacity != -1 && saturatingCapacity < targetKeyCount) {
                targetKeyCount = saturatingCapacity;
                targetThread = thread;
            }
        }
        return targetThread;
    }

    /**
     * 结束操作
     */
    public void shutdown() {
        if (isTerminate) {
            return;
        }
        isTerminate = true;
        for (StealingSelectorThread thread : threads) {
            thread.exit();
        }
    }

    /**
     * 是否已经结束
     *
     * @return true 已结束
     */
    public boolean isTerminate() {
        return isTerminate;
    }

    /**
     * 执行一个任务
     *
     * @param task 任务
     */
    public void execute(IoTask task) {

    }
}
