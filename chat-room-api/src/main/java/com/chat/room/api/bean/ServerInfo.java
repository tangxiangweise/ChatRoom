package com.chat.room.api.bean;

public class ServerInfo {

    private String sn;
    private int port;
    private String address;

    public ServerInfo(String sn, int port, String address) {
        this.sn = sn;
        this.port = port;
        this.address = address;
    }

    public String getSn() {
        return sn;
    }

    public int getPort() {
        return port;
    }

    public String getAddress() {
        return address;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
                "sn='" + sn + '\'' +
                ", port=" + port +
                ", address='" + address + '\'' +
                '}';
    }
    
}
