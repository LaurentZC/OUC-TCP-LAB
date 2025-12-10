package com.ouc.tcp.test.elements;

import com.ouc.tcp.message.TCP_PACKET;

public class WindowElement {
    protected TCP_PACKET tcpPacket = null;
    protected int flag = 0;

    public TCP_PACKET getTcpPacket() {
        return tcpPacket;
    }

    public void setElement(TCP_PACKET packet, int flag) {
        this.tcpPacket = packet;
        this.flag = flag;
    }

    public void reset() {
        tcpPacket = null;
        flag = 0;
    }
}

