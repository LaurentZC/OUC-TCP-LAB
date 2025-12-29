/**
 * 2.1: ACK/NACK
 * Feng Hong; 2015-12-09
 */

package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.TCP_PACKET;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TCP_Receiver extends TCP_Receiver_ADT {
    // 用于记录当前待接收的包序号，注意包序号不完全是
    int sequence = 0;

    /* 构造函数 */
    public TCP_Receiver() {
        // 调用超类构造函数
        super();
        // 初始化 TCP 接收端
        super.initTCP_Receiver(this);
    }

    @Override
    // 接收到数据报：检查校验和，设置回复的 ACK 报文段
    public void rdt_recv(TCP_PACKET recvPack) {
        // 回复的 ACK 报文段
        TCP_PACKET ackPack;
        int dataLen = recvPack.getTcpS().getData().length;
        int dataSeq = (recvPack.getTcpH().getTh_seq() - 1) / dataLen;
        // 检查校验码，生成 ACK
        if (CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
            // 生成 ACK 报文段（设置确认号）
            tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
            // 创建并发送 ACK 包
            ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
            tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
            reply(ackPack);

            // 将接收到的正确有序的数据插入 data 队列，准备交付
            if (dataSeq == sequence) {
                dataQueue.add(recvPack.getTcpS().getData());
                sequence++;
            }
        } else {
            System.out.println("Recieve Computed: " + CheckSum.computeChkSum(recvPack));
            System.out.println("Recieved Packet" + recvPack.getTcpH().getTh_sum());
            System.out.println("Problem: Packet Number: " + recvPack.getTcpH().getTh_seq() + " + InnerSeq:  " + sequence);
            // 不使用 NAK，使用上一个包的序号表达否认
            tcpH.setTh_ack((sequence - 1) * dataLen + 1);
            ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
            tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
            // 回复 ACK 报文段
            reply(ackPack);
        }
        System.out.println();
        // 交付数据（每 20 组数据交付一次）
        if (dataQueue.size() == 20)
            deliver_data();
    }

    @Override
    // 交付数据（将数据写入文件）；不需要修改
    public void deliver_data() {
        // 检查 dataQueue，将数据写入文件
        File fw = new File("recvData.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fw, true))) {
            // 循环检查 data 队列中是否有新交付数据
            while (!dataQueue.isEmpty()) {
                int[] data = dataQueue.poll();

                // 将数据写入文件
                for (int datum : data) {
                    writer.write(datum + "\n");
                }
                // 清空输出缓存
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    // 回复 ACK 报文段
    public void reply(TCP_PACKET replyPack) {
        // 设置错误控制标志
        // eFlag=0，信道无错误
        tcpH.setTh_eflag((byte) 1);
        // 发送数据报
        client.send(replyPack);
    }
}
