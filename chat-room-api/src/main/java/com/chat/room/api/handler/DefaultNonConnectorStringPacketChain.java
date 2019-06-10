package com.chat.room.api.handler;

import com.chat.room.api.box.StringReceivePacket;

/**
 * 默认string接受节点，不做任何事情
 */
public class DefaultNonConnectorStringPacketChain extends  ConnectorStringPacketChain{
    @Override
    protected boolean consume(ClientHandler handler, StringReceivePacket stringReceivePacket) {
        return false;
    }
}
