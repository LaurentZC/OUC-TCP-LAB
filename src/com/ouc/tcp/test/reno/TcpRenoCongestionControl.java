package com.ouc.tcp.test.reno;

import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TcpRenoCongestionControl {
    // 拥塞窗口初始大小为 1
    private int cwnd = 1;
    // 拥塞避免累加器
    private double cwndPrecise = 1.0;
    // 慢启动阈值初始
    private int ssthresh = 16;
    // 拥塞状态
    private TcpRenoState state = TcpRenoState.SLOW_START;

    // CSV记录
    private final PrintWriter csvWriter;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private void logToCsv() {
        if (csvWriter == null) {
            return;
        }
        String timestamp = LocalTime.now().format(timeFormatter);
        csvWriter.printf("%s,%d,%d,%s%n", timestamp, cwnd, ssthresh, state);
        csvWriter.flush();
    }

    public TcpRenoCongestionControl(PrintWriter csvWriter) {
        this.csvWriter = csvWriter;
        logToCsv();
    }

    public void onAck(int acked) {
        if (state == TcpRenoState.SLOW_START) {
            acked = slowStart(acked);
            if (acked == 0) {
                return;
            }
        }

        if (state == TcpRenoState.CONGESTION_AVOIDANCE && acked > 0) {
            congestionAvoidance(acked);
        }
    }

    private int slowStart(int acked) {
        int newCwnd = Math.min(cwnd + acked, ssthresh);
        int usedAcked = newCwnd - cwnd;

        cwnd = Math.min(newCwnd, cwnd * 2);
        cwndPrecise = cwnd;
        acked -= usedAcked;

        // 如果达到 ssthresh，进入拥塞避免阶段
        if (cwnd >= ssthresh) {
            state = TcpRenoState.CONGESTION_AVOIDANCE;
            cwnd = ssthresh;
            cwndPrecise = cwnd;
        }

        logToCsv();
        return acked;
    }

    private void congestionAvoidance(int acked) {
        cwndPrecise += (double) acked / cwnd;
        cwnd = (int) cwndPrecise;
        logToCsv();
    }

    public void onTimeout() {
        ssthresh = Math.max(cwnd / 2, 2);
        cwnd = 1;
        cwndPrecise = 1.0;
        state = TcpRenoState.SLOW_START;
        logToCsv();
    }

    public void onFastRetransmit() {
        // 更新拥塞窗口和阈值
        ssthresh = Math.max(cwnd / 2, 2);
        cwnd = ssthresh;
        cwndPrecise = cwnd;
        // 为了打个日志
        state = TcpRenoState.FAST_RETRANSMIT;
        logToCsv();
        // 快重传后进入拥塞避免
        state = TcpRenoState.CONGESTION_AVOIDANCE;
    }

    public int getCwnd() {
        return cwnd;
    }
}
