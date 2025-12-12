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
import java.util.Iterator;
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

    private void logToCsv() {
        if (csvWriter == null) {
            return;
        }
        String timestamp = LocalTime.now().format(timeFormatter);
        csvWriter.printf("%s,%d,%d%n", timestamp, cwnd, ssthresh);
        csvWriter.flush();
    }

    public SenderWindow(TCP_Sender sender) {
        this.sender = sender;
        this.timer = new UDT_Timer();
        try {
            csvWriter = new PrintWriter(new FileWriter("cwnd_ssthresh.csv", false));
            csvWriter.println("Time,cwnd,ssthresh");
            logToCsv();
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
        timer.cancel();
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
        window.push(new SenderElement(packet, SenderElementFlag.NOT_ACKED.ordinal()));
    }


    public void ackPacket(int ack) {
        Iterator<SenderElement> iterator = window.iterator();
        while (iterator.hasNext()) {
            SenderElement element = iterator.next();
            if (element.getTcpPacket().getTcpH().getTh_seq() > ack) {
                continue;
            }
            element.ackPacket();
            iterator.remove();
            if (cwnd < ssthresh) {
                cwnd++;
                cwndPrecise = cwnd;
            }
            resetTimer();
        }

        if (cwnd >= ssthresh) {
            cwndPrecise += 1.0 / cwnd;
            cwnd = (int) cwndPrecise;
        }

        logToCsv();

        // 检测重复 ACK
        if (ack == lastAck) {
            dupAckCount++;
            System.out.println("Duplicate ACK " + dupAckCount + " for seq: " + ack);
        } else {
            lastAck = ack;
            dupAckCount = 1;
        }
    }

    public void handleTimeout() {
        System.out.println("Timeout occurred. Retransmitting all packets.\n");

        // 更新 cwnd 和 ssthresh
        ssthresh = Math.max(cwnd / 2, 2);
        cwnd = 1;
        cwndPrecise = cwnd;

        for (SenderElement element : window) {
            if (!element.isAcked()) {
                sender.udt_send(element.getTcpPacket());
            }
        }

        resetTimer();
        logToCsv();
    }
}