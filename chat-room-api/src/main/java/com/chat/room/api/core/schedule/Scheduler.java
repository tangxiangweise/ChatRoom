package com.chat.room.api.core.schedule;

import java.io.Closeable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 任务调度者
 */
public interface Scheduler extends Closeable {

    /**
     * 调度一份延迟任务
     *
     * @param runnable 任务
     * @param delay    延迟时间
     * @param unit     延迟时间单位
     * @return 返回调度任务的控制Future
     */
    ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit);

    void delivery(Runnable runnable);

}
