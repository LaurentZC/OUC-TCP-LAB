package com.ouc.tcp.test.windows;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.test.GBN_RetransTask;
import com.ouc.tcp.test.TCP_Sender;
import com.ouc.tcp.test.elements.SenderElement;
import com.ouc.tcp.test.elements.SenderElementFlag;
import com.ouc.tcp.test.reno.TcpRenoCongestionControl;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * 发送方滑动窗口
 * 管理待发送、已发送但未确认、已确认的TCP数据包
 */
public class SenderWindow {
    private final LinkedList<SenderElement> window = new LinkedList<>();

    private final TcpRenoCongestionControl congestion;
    private final TCP_Sender sender;
    private UDT_Timer timer;
    private static final int DELAY = 3000;
    private static final int PERIOD = 3000;

    private int lastAck = -1;
    private int dupAckCount = 0;
    private static final int DUP_ACK_THRESHOLD = 3;

    public SenderWindow(TCP_Sender sender) {
        this.sender = sender;
        this.timer = new UDT_Timer();
        try {
            PrintWriter csvWriter = new PrintWriter(new FileWriter("cwnd_ssthresh.csv", false));
            csvWriter.println("Time,cwnd,ssthresh,state");
            this.congestion = new TcpRenoCongestionControl(csvWriter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isEmpty() {
        return window.isEmpty();
    }

    public boolean isCwndFull() {
        return window.size() >= congestion.getCwnd();
    }

    private void resetTimer() {
        timer.cancel();
        if (!isEmpty()) {
            timer = new UDT_Timer();
            timer.schedule(new GBN_RetransTask(this), DELAY, PERIOD);
        }
    }

    public void pushTcpPacket(TCP_PACKET packet) {
        while (isCwndFull()) {
            Thread.onSpinWait();
        }

        if (isEmpty()) {
            timer = new UDT_Timer();
            timer.schedule(new GBN_RetransTask(this), DELAY, PERIOD);
        }
        sender.udt_send(packet);
        window.offerLast(new SenderElement(packet, SenderElementFlag.NOT_ACKED.ordinal()));
    }


    public void ackPacket(int ack) {
        int acked = 0;
        Iterator<SenderElement> iterator = window.iterator();
        while (iterator.hasNext()) {
            SenderElement element = iterator.next();
            if (element.getTcpPacket().getTcpH().getTh_seq() > ack) {
                break;
            }
            element.ackPacket();
            iterator.remove();
            acked++;
            resetTimer();
        }

        // 更新拥塞窗口
        if (acked > 0) {
            congestion.onAck(acked);
        }

        // 处理重复ACK
        if (ack == lastAck) {
            handleDupAck(ack);
        } else {
            lastAck = ack;
            dupAckCount = 1;
        }
    }

    private void handleDupAck(int ack) {
        // 记录 ACK
        dupAckCount++;
        System.out.println("Duplicate ACK " + dupAckCount + " for seq: " + ack);

        // 快重传
        if (dupAckCount >= DUP_ACK_THRESHOLD) {
            System.out.println("fast retransmit for seq = " + ack);
            congestion.onFastRetransmit();
            fastRetransmit(ack);
        }
    }

    private void fastRetransmit(int ack) {
        int expectedSeq = ack + 100;
        for (SenderElement element : window) {
            int seq = element.getTcpPacket().getTcpH().getTh_seq();
            if (seq == expectedSeq) {
                sender.udt_send(element.getTcpPacket());
                System.out.println("Fast retransmit packet with seq: " + seq);
                break;
            }
        }
    }

    public void handleTimeout() {
        System.out.println("Timeout occurred. Retransmitting all packets.\n");

        congestion.onTimeout();

        if (timer != null) {
            timer.cancel();
        }

        for (SenderElement element : window) {
            if (!element.isAcked()) {
                sender.udt_send(element.getTcpPacket());
                timer = new UDT_Timer();
                timer.schedule(new GBN_RetransTask(this), DELAY, PERIOD);
                break;
            }
        }
    }
}