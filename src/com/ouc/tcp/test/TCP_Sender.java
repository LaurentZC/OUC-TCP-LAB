/**
 * 2.1: ACK/NACK
 * Feng Hong; 2015-12-09
 */

package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.test.windows.SenderWindow;

public class TCP_Sender extends TCP_Sender_ADT {
    // 待发送的 TCP 数据报
    TCP_PACKET tcpPack;
    // 滑动窗口刚开始不满
    private volatile int flag = 1;
    // 发送者窗口
    private final SenderWindow window;

    /* 构造函数 */
    public TCP_Sender() {
        // 调用超类构造函数
        super();
        // 初始化 TCP 发送端
        super.initTCP_Sender(this);
        this.window = new SenderWindow(this);
    }

    @Override
    // 可靠发送（应用层调用）：封装应用层数据，产生 TCP 数据报；需要修改
    public void rdt_send(int dataIndex, int[] appData) {
        // 生成 TCP 数据报（设置序号和数据字段/校验和),注意打包的顺序
        // 包序号设置为字节流号：
        tcpH.setTh_seq(dataIndex * appData.length + 1);
        tcpS.setData(appData);
        tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);
        // 更新带有 checksum 的 TCP 报文头
        tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
        tcpPack.setTcpH(tcpH);

        // 如果窗口满，等待窗口有空间
        if (window.isCwndFull()) {
            flag = 0;
        }

        while (flag == 0) {
            Thread.yield();
        }

        try {
            window.pushTcpPacket(tcpPack.clone());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        window.sendTcpPacket();
    }

    @Override
    // 不可靠发送：将打包好的 TCP 数据报通过不可靠传输信道发送；仅需修改错误标志
    public void udt_send(TCP_PACKET stcpPack) {
        // 设置错误控制标志
        tcpH.setTh_eflag((byte) 7);
        // System.out.println("to send: "+stcpPack.getTcpH().getTh_seq());
        // 发送数据报
        client.send(stcpPack);
    }

    @Override
    // 需要修改
    public void waitACK() {
        // 循环检查 ackQueue
        // 循环检查确认号对列中是否有新收到的 ACK
        if (!ackQueue.isEmpty()) {
            int curAck = ackQueue.poll();
            window.ackPacket(curAck);
        }
        if (!window.isCwndFull()) {
            flag = 1;
        }
    }

    @Override
    // 接收到 ACK 报文：检查校验和，将确认号插入 ack 队列; NACK 的确认号为 -1；不需要修改
    public void recv(TCP_PACKET recvPack) {
        System.out.println("Receive ACK Number： " + recvPack.getTcpH().getTh_ack());
        ackQueue.add(recvPack.getTcpH().getTh_ack());
        System.out.println();

        // 处理 ACK 报文
        waitACK();
    }
}
