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

    private void logToCsv(String message) {
        if (csvWriter == null) {
            return;
        }
        if (message == null) {
            message = state.toString();
        }
        String timestamp = LocalTime.now().format(timeFormatter);
        csvWriter.printf("%s,%d,%d,%s%n", timestamp, cwnd, ssthresh, message);
        csvWriter.flush();
    }

    public TcpRenoCongestionControl(PrintWriter csvWriter) {
        this.csvWriter = csvWriter;
        logToCsv(null);
    }

    public void onAck(int acked) {
        if (state == TcpRenoState.SLOW_START) {
            acked = slowStart(acked);
            if (acked == 0) {
                return;
            }
        }
        state = TcpRenoState.CONGESTION_AVOIDANCE;
        congestionAvoidance(acked);
    }

    private int slowStart(int acked) {
        int newCwnd = Math.min(cwnd + acked, ssthresh);
        int usedAcked = newCwnd - cwnd;

        cwnd = Math.min(newCwnd, cwnd * 2);
        cwndPrecise = cwnd;
        logToCsv(null);
        return acked - usedAcked;
    }

    private void congestionAvoidance(int acked) {
        cwndPrecise += (double) acked / cwnd;
        cwnd = (int) cwndPrecise;
        logToCsv(null);
    }

    public void onTimeout() {
        ssthresh = Math.max(cwnd / 2, 2);
        cwnd = 1;
        cwndPrecise = 1.0;
        state = TcpRenoState.SLOW_START;
        logToCsv(null);
    }

    public void onFastRetransmit() {
        // 更新拥塞窗口和阈值
        ssthresh = Math.max(cwnd / 2, 2);
        cwnd = ssthresh;
        cwndPrecise = cwnd;
        // 快重传后进入拥塞避免
        state = TcpRenoState.CONGESTION_AVOIDANCE;
        logToCsv("fast retransmit");
    }

    public int getCwnd() {
        return cwnd;
    }
}
