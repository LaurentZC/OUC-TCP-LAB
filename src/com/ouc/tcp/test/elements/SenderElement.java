package com.ouc.tcp.test.elements;

public class SenderElement extends WindowElement {

    public SenderElement() {
        super();
    }

    public boolean isAcked() {
        return this.flag == SenderElementFlag.ACKED.ordinal();
    }

    public void ackPacket() {
        this.flag = SenderElementFlag.ACKED.ordinal();
    }
}
