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
        String timestamp = LocalTime.now().format(timeFormatter);
        csvWriter.printf("%s,%d,%d,%s%n", timestamp, cwnd, ssthresh, state.toString());
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
        state = TcpRenoState.CONGESTION_AVOIDANCE;
        congestionAvoidance(acked);
    }

    private int slowStart(int acked) {
        int newCwnd = Math.min(cwnd + acked, ssthresh);
        int usedAcked = newCwnd - cwnd;
        cwnd = newCwnd;
        cwndPrecise = cwnd;
        logToCsv();
        return acked - usedAcked;
    }

    private void congestionAvoidance(int acked) {
        cwndPrecise += (double) acked / cwnd;
        cwnd = (int) cwndPrecise;
        logToCsv();
    }

    public void onTimeout() {
        state = TcpRenoState.SLOW_START;
        ssthresh = Math.max(cwnd / 2, 2);
        cwnd = 1;
        cwndPrecise = 1.0;
        logToCsv();
    }

    public void onFastRetransmit() {
        // 更新拥塞窗口和阈值
        ssthresh = Math.max(cwnd / 2, 2);
        cwnd = ssthresh;
        cwndPrecise = cwnd;
        state = TcpRenoState.FAST_RECOVERY;
        logToCsv();
    }

    public void onFastRecovery() {
        state = TcpRenoState.FAST_RECOVERY;
        cwnd++;
        cwndPrecise = cwnd;
        logToCsv();
    }

    public void endFastRecovery() {
        if (state != TcpRenoState.FAST_RECOVERY) {
            return;
        }
        state = TcpRenoState.CONGESTION_AVOIDANCE;
        cwnd = ssthresh;
        cwndPrecise = cwnd;
        logToCsv();
    }

    public int getCwnd() {
        return cwnd;
    }
}
