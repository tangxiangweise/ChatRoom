package com.chat.room.api.bean;

import com.chat.room.api.box.StringReceivePacket;
import com.chat.room.api.handler.ConnectorHandler;
import com.chat.room.api.handler.ConnectorStringPacketChain;

import java.util.ArrayList;
import java.util.List;

public class Group {

    private final String name;
    private final GroupMessageAdapter adapter;
    private final List<ConnectorHandler> members = new ArrayList<>();

    public Group(String name, GroupMessageAdapter adapter) {
        this.name = name;
        this.adapter = adapter;
    }

    public String getName() {
        return name;
    }

    public boolean addMember(ConnectorHandler handler) {
        synchronized (members) {
            if (!members.contains(handler)) {
                members.add(handler);
                handler.getStringPacketChain().appendLast(new ForwardConnectorStringPacketChain());
                System.out.println(" Group [" + name + "] add new Member:" + handler.getClientInfo());
                return true;
            }
            return false;
        }
    }

    public boolean removeMember(ConnectorHandler handler) {
        synchronized (members) {
            if (members.remove(handler)) {
                handler.getStringPacketChain().remove(ForwardConnectorStringPacketChain.class);
                System.out.println(" Group [" + name + "] leave new Member:" + handler.getClientInfo());
                return true;
            }
            return false;
        }
    }

    private class ForwardConnectorStringPacketChain extends ConnectorStringPacketChain {
        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket packet) {
            synchronized (members) {
                for (ConnectorHandler member : members) {
                    if (member == handler) {
                        continue;
                    }
                    adapter.sendMessageToClient(member, packet.entity());
                }
                return true;
            }
        }
    }

    public interface GroupMessageAdapter {

        void sendMessageToClient(ConnectorHandler handler, String msg);

    }
}
