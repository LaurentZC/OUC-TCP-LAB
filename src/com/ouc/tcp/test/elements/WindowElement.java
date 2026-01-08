package com.ouc.tcp.test.elements;

import com.ouc.tcp.message.TCP_PACKET;

public class WindowElement {
    protected TCP_PACKET tcpPacket = null;

    protected WindowElement() {
    }

    public TCP_PACKET getTcpPacket() {
        return this.tcpPacket;
    }
}
