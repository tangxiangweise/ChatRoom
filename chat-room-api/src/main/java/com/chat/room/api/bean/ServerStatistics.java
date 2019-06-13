package com.chat.room.api.bean;

import com.chat.room.api.box.StringReceivePacket;
import com.chat.room.api.handler.ConnectorHandler;
import com.chat.room.api.handler.ConnectorStringPacketChain;


public class ServerStatistics {

    public long receiveSize;
    public long sendSize;

    public ConnectorStringPacketChain statisticsChain() {
        return new StatisticsConnectorStringPacketChain();
    }

    class StatisticsConnectorStringPacketChain extends ConnectorStringPacketChain {
        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket packet) {
            receiveSize++;
            return false;
        }
    }

}
