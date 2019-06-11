package com.chat.room.api.core.schedule;

import com.chat.room.api.core.Connector;

import java.util.concurrent.TimeUnit;

public class IdleTimeoutScheduleJob extends ScheduleJob {

    public IdleTimeoutScheduleJob(long idleTime, TimeUnit unit, Connector connector) {
        super(idleTime, unit, connector);
    }

    @Override
    public void run() {
        long lastActiveTime = connector.getLastActiveTime();
        long idleTimeoutMilliseconds = this.idleTimeoutMilliseconds;
        // 下一次调度任务的延迟时间 空闲时间：50 当前： 100 最后的活跃时间：80
        long nextDelay = idleTimeoutMilliseconds - (System.currentTimeMillis() - lastActiveTime);
        if (nextDelay <= 0) {
            // 过期 原idleTimeoutMilliseconds执行
            schedule(idleTimeoutMilliseconds);
            try {
                //发送心跳包
                connector.fireIdleTimeoutEvent();
            } catch (Throwable throwable) {
                connector.fireExceptionCaught(throwable);
            }
        } else {
            // 剩余时间再执行任务
            schedule(nextDelay);
        }
    }

}
