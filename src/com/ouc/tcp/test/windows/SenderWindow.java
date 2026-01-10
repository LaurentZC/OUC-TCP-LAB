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
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 发送方滑动窗口
 * 管理待发送、已发送但未确认、已确认的TCP数据包
 */
public class SenderWindow {
    private final LinkedBlockingDeque<SenderElement> window = new LinkedBlockingDeque<>();
    private final LinkedBlockingDeque<SenderElement> cache = new LinkedBlockingDeque<>();
    private static final int MAX_CACHE_SIZE = 8;

    private final TcpRenoCongestionControl congestion;
    private final TCP_Sender sender;
    private UDT_Timer timer;
    private static final int DELAY = 1500;
    private static final int PERIOD = 1500;

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

    public boolean isFull() {
        return cache.size() >= MAX_CACHE_SIZE;
    }

    private void resetTimer() {
        timer.cancel();
        timer = new UDT_Timer();
        if (!window.isEmpty()) {
            timer.schedule(new GBN_RetransTask(this), DELAY, PERIOD);
        }
    }

    public void pushTcpPacket(TCP_PACKET packet) {
        cache.offerLast(new SenderElement(packet, SenderElementFlag.NOT_ACKED.ordinal()));
        trySendPackets();
    }

    public void ackPacket(int ack) {
        int acked = 0;
        while (true) {
            SenderElement element = window.peekFirst();
            if (element != null && element.getTcpPacket().getTcpH().getTh_seq() <= ack) {
                element.ackPacket();
                window.pollFirst();
                acked++;
                resetTimer();
            } else {
                break;
            }
        }

        if (ack == lastAck) {
            handleDupAck(ack);
            return;
        }
        lastAck = ack;
        dupAckCount = 1;
        congestion.endFastRecovery();

        // 更新拥塞窗口
        if (acked > 0) {
            congestion.onAck(acked);
            updateWindow();
            trySendPackets();
        }
    }

    private void handleDupAck(int ack) {
        // 记录 ACK 重复数
        dupAckCount++;
        // 快重传
        if (dupAckCount == DUP_ACK_THRESHOLD) {
            congestion.onFastRetransmit();
            fastRetransmit(ack);
        } else if (dupAckCount > DUP_ACK_THRESHOLD) {
            congestion.onFastRecovery();
            trySendPackets();
        }
    }

    private void fastRetransmit(int ack) {
        int expectedSeq = ack + 100;
        for (SenderElement element : window) {
            if (element.getTcpPacket().getTcpH().getTh_seq() == expectedSeq) {
                sender.udt_send(element.getTcpPacket());
                updateWindow();
                break;
            }
        }
    }

    private synchronized void updateWindow() {
        int cwnd = congestion.getCwnd();

        while (window.size() > cwnd) {
            SenderElement removed = window.pollLast();
            if (removed != null) {
                cache.offerFirst(removed);
            }
        }
    }

    public void handleTimeout() {
        timer.cancel();
        congestion.onTimeout();
        updateWindow();
        SenderElement retransmitElement = window.peekFirst();
        if (retransmitElement != null) {
            sender.udt_send(retransmitElement.getTcpPacket());
        }
        resetTimer();
    }

    public synchronized void trySendPackets() {
        while (!cache.isEmpty() && window.size() < congestion.getCwnd()) {
            SenderElement element = cache.pollFirst();
            if (element == null || element.getTcpPacket().getTcpH().getTh_seq() <= lastAck) {
                continue;
            }
            sender.udt_send(element.getTcpPacket());
            window.offerLast(element);
            if (window.size() == 1) {
                resetTimer();
            }
        }
    }
}