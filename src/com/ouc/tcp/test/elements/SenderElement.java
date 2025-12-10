package com.ouc.tcp.test.elements;

import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;

public class SenderElement extends WindowElement {
    private UDT_Timer timer;

    public SenderElement() {
        super();
        this.timer = null;
    }

    public boolean isAcked() {
        return this.flag == SenderElementFlag.ACKED.ordinal();
    }

    public void scheduleTask(UDT_RetransTask task, int delay, int period) {
        this.timer = new UDT_Timer();
        this.timer.schedule(task, delay, period);
    }

    public void ackPacket() {
        this.flag = SenderElementFlag.ACKED.ordinal();
        this.timer.cancel();
    }
}
