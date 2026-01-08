package com.ouc.tcp.test.windows;

/**
 * 数据包确认状态枚举
 * ORDERED: 有序到达 - 数据包按顺序到达但不是窗口基序号
 * DISORDERED: 无序到达 - 数据包超出窗口范围
 * DUPLICATE: 重复到达 - 数据包序号小于窗口基序号
 */
public enum AckState {
    BASE,
    ORDERED,
    DUPLICATE,
    OUTOFWINDOW;

    @Override
    public String toString() {
        return switch (this) {
            case BASE -> "窗口基序号";
            case ORDERED -> "有序到达";
            case DUPLICATE -> "重复到达";
            case OUTOFWINDOW -> "超出窗口范围";
        };
    }
}