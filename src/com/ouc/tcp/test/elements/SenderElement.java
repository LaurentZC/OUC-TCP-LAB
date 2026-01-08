package com.ouc.tcp.test.elements;

import com.ouc.tcp.message.TCP_PACKET;

public class SenderElement extends WindowElement {
    private int flag;

    public SenderElement(TCP_PACKET packet, int flag) {
        super();
        this.tcpPacket = packet;
        this.flag = flag;
    }

    public boolean isAcked() {
        return this.flag == SenderElementFlag.ACKED.ordinal();
    }

    public void ackPacket() {
        this.flag = SenderElementFlag.ACKED.ordinal();
    }
}
