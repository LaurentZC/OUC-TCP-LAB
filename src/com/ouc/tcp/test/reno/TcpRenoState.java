package com.ouc.tcp.test.reno;

public enum TcpRenoState {
    SLOW_START,
    CONGESTION_AVOIDANCE;

    @Override
    public String toString() {
        return switch (this) {
            case SLOW_START -> "slow start";
            case CONGESTION_AVOIDANCE -> "congestion avoidance";
        };
    }
}
