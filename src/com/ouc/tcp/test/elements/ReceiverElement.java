package com.ouc.tcp.test.elements;

public class ReceiverElement extends WindowElement {
    public ReceiverElement() {
        super();
    }

    public boolean isBuffered() {
        return this.flag == ReceiverElementFlag.BUFFERED.ordinal();
    }
}
