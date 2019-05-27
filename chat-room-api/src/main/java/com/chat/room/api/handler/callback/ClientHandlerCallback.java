package com.chat.room.api.handler.callback;

import com.chat.room.api.handler.ClientHandler;

public interface ClientHandlerCallback {

    /**
     * 自身关闭
     *
     * @param handler
     */
    void onSelfClosed(ClientHandler handler);

    /**
     * 收到消息时通知
     *
     * @param handler
     * @param msg
     */
    void onNewMessageArrived(ClientHandler handler, String msg);

}
