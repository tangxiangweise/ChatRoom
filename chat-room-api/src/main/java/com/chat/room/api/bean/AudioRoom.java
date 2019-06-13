package com.chat.room.api.bean;

import com.chat.room.api.handler.ConnectorHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 房间的基本封装
 */
public class AudioRoom {

    private final String roomCode;

    private volatile ConnectorHandler handler1;

    private volatile ConnectorHandler handler2;

    public AudioRoom() {
        this.roomCode = getRandomString(5);
    }

    public String getRoomCode() {
        return roomCode;
    }

    public ConnectorHandler[] getConnectors() {
        List<ConnectorHandler> handlers = new ArrayList<>(2);
        if (handler1 != null) {
            handlers.add(handler1);
        }
        if (handler2 != null) {
            handlers.add(handler2);
        }
        return handlers.toArray(new ConnectorHandler[0]);
    }

    /**
     * 获取对方
     *
     * @param handler
     * @return
     */
    public ConnectorHandler getTheOtherHandler(ConnectorHandler handler) {
        return (handler1 == handler || handler1 == null) ? handler2 : handler1;
    }

    /**
     * 房间是否可聊天，是否两个客户端都具有
     *
     * @return
     */
    public synchronized boolean isEnable() {
        return handler1 != null && handler2 != null;
    }

    /**
     * 加入房间
     *
     * @param handler
     * @return
     */
    public synchronized boolean enterRoom(ConnectorHandler handler) {
        if (handler1 == null) {
            this.handler1 = handler;
        } else if (handler2 == null) {
            this.handler2 = handler;
        } else {
            return false;
        }
        return true;
    }

    /**
     * 退出房间
     * @param handler 退出后如果还有一个剩余则返回剩余的人
     * @return
     */
    public synchronized ConnectorHandler exitRoom(ConnectorHandler handler) {
        if (handler1 == handler) {
            handler1 = null;
        } else if (handler2 == handler) {
            handler2 = null;
        }
        return handler1 == null ? handler2 : handler1;
    }

    private String getRandomString(int length) {
        final String str = "123456789";
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            int number = random.nextInt(str.length());
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

}
