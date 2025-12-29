package com.ouc.tcp.test.windows;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.test.GBN_RetransTask;
import com.ouc.tcp.test.TCP_Sender;
import com.ouc.tcp.test.elements.SenderElement;
import com.ouc.tcp.test.elements.SenderElementFlag;

/**
 * 发送方滑动窗口
 * 管理待发送、已发送但未确认、已确认的TCP数据包
 */
public class SenderWindow extends SlidingWindow<SenderElement> {
    private int nextToSend;  // 下一个待发送的包序号（窗口内的相对位置）
    private int rear;        // 窗口尾序号，表示最后一个已添加到窗口的数据包序号 + 1
    private final TCP_Sender sender;
    private UDT_Timer timer; // 定时器，用于管理重传任务
    private final int delay;       // 重传延迟时间
    private final int period;      // 重传周期

    /**
     * 构造函数
     *
     * @param sender 发送方实例
     * @param size   窗口大小
     * @param delay  重传延迟时间
     * @param period 重传周期
     */
    public SenderWindow(TCP_Sender sender, int size, int delay, int period) {
        super(size);
        this.nextToSend = 0;  // 初始时，下一个待发送的包序号为 0
        this.rear = 0;        // 初始时，窗口尾序号为 0
        this.sender = sender;
        this.timer = new UDT_Timer();
        this.delay = delay;
        this.period = period;
    }

    @Override
    protected SenderElement[] createWindowArray(int size) {
        return new SenderElement[size];
    }

    @Override
    protected SenderElement createElement(int index) {
        return new SenderElement();
    }

    /**
     * 判断窗口是否已满
     *
     * @return 窗口满的条件：窗口尾序号与基序号之差等于窗口大小
     */
    public boolean isFull() {
        return rear - base == size;
    }

    /**
     * 判断窗口是否为空
     *
     * @return 窗口空的条件：基序号等于窗口尾序号
     */
    public boolean isEmpty() {
        return base == rear;
    }

    /**
     * 检查窗口内所有数据包是否都已发送
     *
     * @return 如果下一个待发送序号等于窗口尾序号，表示全部已发送
     */
    public boolean isAllSent() {
        return nextToSend == rear;
    }

    /**
     * 将TCP数据包添加到窗口
     *
     * @param packet 待发送的TCP数据包
     */
    public void pushTcpPacket(TCP_PACKET packet) {
        // 获取窗口尾位置对应的数组索引
        int idx = getIdx(rear);
        // 设置窗口元素：存储数据包，标记为未确认
        window[idx].setElement(packet, SenderElementFlag.NOT_ACKED.ordinal());
        rear++;  // 窗口尾序号后移
    }

    /**
     * 发送下一个待发送的TCP数据包
     */
    public void sendTcpPacket() {
        // 如果窗口为空或所有包都已发送，则直接返回
        if (isEmpty() || isAllSent()) {
            return;
        }
        // 获取下一个待发送包在窗口中的索引
        int idx = getIdx(nextToSend);
        TCP_PACKET packet = window[idx].getTcpPacket();
        // 如果发送的是窗口中的第一个包，启动定时器
        if (nextToSend == base) {
            timer.schedule(new GBN_RetransTask(this), delay, period);
        }
        // 更新下一个待发送序号
        nextToSend++;
        // 发送数据包
        sender.udt_send(packet);
    }

    /**
     * 发送窗口内所有未发送的 TCP 数据包
     */
    public void sendAllPacket() {
        nextToSend = base;
        while (nextToSend < rear) {
            sendTcpPacket();
        }
    }

    /**
     * 处理接收到的 ACK 确认
     *
     * @param seq 被确认的数据包序列号
     */
    public void ackTcpPacket(int seq) {
        // 遍历窗口内所有已发送但未确认的数据包
        for (int i = base; i != rear; i++) {
            int idx = getIdx(i);
            // 找到序列号匹配且未确认的数据包
            if (window[idx].getTcpPacket().getTcpH().getTh_seq() > seq || window[idx].isAcked()) {
                continue;
            }
            window[idx].ackPacket();  // 标记为已确认
            window[idx].reset();
            base++; // 滑动窗口
            resetTimer();
        }
    }

    /**
     * 重置定时器, 以便在窗口内有未确认的数据包时继续定时重传
     */
    private void resetTimer() {
        timer.cancel();
        timer = new UDT_Timer();
        timer.schedule(new GBN_RetransTask(this), delay, period);
    }
}