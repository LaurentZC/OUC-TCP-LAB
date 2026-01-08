package com.ouc.tcp.test.elements;

import com.ouc.tcp.message.TCP_PACKET;

public class ReceiverElement extends WindowElement {
    boolean buffered = false;

    public ReceiverElement() {
        super();
    }

    public void setElement(TCP_PACKET packet, boolean buffered) {
        this.tcpPacket = packet;
        this.buffered = buffered;
    }

    public void reset() {
        this.tcpPacket = null;
        this.buffered = false;
    }

    public boolean isBuffered() {
        return buffered;
    }
}
