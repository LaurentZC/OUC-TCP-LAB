package com.ouc.tcp.test.windows;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.test.GBN_RetransTask;
import com.ouc.tcp.test.TCP_Sender;
import com.ouc.tcp.test.elements.SenderElement;
import com.ouc.tcp.test.elements.SenderElementFlag;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 发送方滑动窗口
 * 管理待发送、已发送但未确认、已确认的TCP数据包
 */
public class SenderWindow {
    private final LinkedBlockingDeque<SenderElement> window = new LinkedBlockingDeque<>();

    // 拥塞窗口初始大小为 1
    private int cwnd = 1;
    // 拥塞避免累加器
    private double cwndPrecise = 1.0;
    // 慢启动阈值初始
    private int ssthresh = 16;

    private final TCP_Sender sender;
    private UDT_Timer timer;
    private static final int DELAY = 3000;
    private static final int PERIOD = 3000;

    private int lastAck = -1;
    private int dupAckCount = 0;
    private static final int DUP_ACK_THRESHOLD = 3;

    // CSV记录
    private final PrintWriter csvWriter;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private void logToCsv(String state) {
        if (csvWriter == null) {
            return;
        }
        String timestamp = LocalTime.now().format(timeFormatter);
        csvWriter.printf("%s,%d,%d,%s%n", timestamp, cwnd, ssthresh, state);
        csvWriter.flush();
    }

    public SenderWindow(TCP_Sender sender) {
        this.sender = sender;
        this.timer = new UDT_Timer();
        try {
            csvWriter = new PrintWriter(new FileWriter("cwnd_ssthresh.csv", false));
            csvWriter.println("Time,cwnd,ssthresh,state");
            logToCsv("slow start");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isEmpty() {
        return window.isEmpty();
    }

    public boolean isCwndFull() {
        return window.size() >= cwnd;
    }

    private void resetTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new UDT_Timer();
        if (!isEmpty()) {
            timer.schedule(new GBN_RetransTask(this), DELAY, PERIOD);
        }
    }

    public void pushTcpPacket(TCP_PACKET packet) {
        if (isEmpty()) {
            timer = new UDT_Timer();
            timer.schedule(new GBN_RetransTask(this), DELAY, PERIOD);
        }
        sender.udt_send(packet);
        window.offerLast(new SenderElement(packet, SenderElementFlag.NOT_ACKED.ordinal()));
    }


    public void ackPacket(int ack) {
        boolean slowStart = false;
        boolean congestionAvoidance = false;
        while (!window.isEmpty()) {
            SenderElement element = window.peekFirst();
            if (element.getTcpPacket().getTcpH().getTh_seq() > ack) {
                break;
            }
            element.ackPacket();
            window.pollFirst();
            if (cwnd < ssthresh) {
                slowStart = true;
                cwnd++;
                cwndPrecise = cwnd;
            }
            resetTimer();
        }

        if (slowStart) {
            logToCsv("slow start");
        }

        if (cwnd >= ssthresh) {
            congestionAvoidance = true;
            cwndPrecise += 1.0;
            cwnd = (int) cwndPrecise;
        }

        if (congestionAvoidance) {
            logToCsv("congestion avoidance");
        }

        handleDupAck(ack);
    }

    private void handleDupAck(int ack) {
        // 检测重复 ACK
        if (ack == lastAck) {
            dupAckCount++;
            System.out.println("Duplicate ACK " + dupAckCount + " for seq: " + ack);
        } else {
            lastAck = ack;
            dupAckCount = 0;
        }

        // 快重传
        if (dupAckCount >= DUP_ACK_THRESHOLD) {
            System.out.println("fast retransmit for seq = " + ack);
            ssthresh = Math.max(cwnd / 2, 2);
            cwnd = ssthresh;
            cwndPrecise = cwnd;
            fastRetransmit(ack);
            logToCsv("fast retransmit");
        }
    }

    private void fastRetransmit(int ack) {
        int expectedSeq = ack + 100;
        for (SenderElement element : window) {
            int seq = element.getTcpPacket().getTcpH().getTh_seq();
            if (seq == expectedSeq || (seq > expectedSeq && !element.isAcked())) {
                sender.udt_send(element.getTcpPacket());
                System.out.println("Fast retransmit packet with seq: " + seq);
                break;
            }
        }
    }

    public void handleTimeout() {
        System.out.println("Timeout occurred. Retransmitting all packets.\n");

        // 更新 cwnd 和 ssthresh
        ssthresh = Math.max(cwnd / 2, 2);
        cwnd = 1;
        cwndPrecise = cwnd;

        if (timer != null) {
            timer.cancel();
        }

        for (SenderElement element : window) {
            if (!element.isAcked()) {
                sender.udt_send(element.getTcpPacket());
            }
        }

        if (!window.isEmpty()) {
            timer = new UDT_Timer();
            timer.schedule(new GBN_RetransTask(this), DELAY, PERIOD);
        }
        logToCsv("time out");
    }
}