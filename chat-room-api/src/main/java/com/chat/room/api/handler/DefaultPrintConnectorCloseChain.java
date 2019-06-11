package com.chat.room.api.handler;

import com.chat.room.api.core.Connector;

/**
 * 关闭链接链式结构
 */
public class DefaultPrintConnectorCloseChain extends ConnectorCloseChain {

    @Override
    protected boolean consume(ConnectorHandler handler, Connector connector) {
        System.out.println(handler.getClientInfo() + " : Exit!! , key : " + handler.getKey());
        return false;
    }

}
