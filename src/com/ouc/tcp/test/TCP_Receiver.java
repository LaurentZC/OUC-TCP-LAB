/**
 * 2.1: ACK/NACK
 * Feng Hong; 2015-12-09
 */

package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.test.windows.AckState;
import com.ouc.tcp.test.windows.ReceiverWindow;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TCP_Receiver extends TCP_Receiver_ADT {
    // 接收者窗口
    private final ReceiverWindow window = new ReceiverWindow(16);

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
        // 回复的 ACK 报文段（在需要时构造并发送）
        TCP_PACKET ackPack;
        // 校验和检查：若校验失败则丢弃该报文段
        if (CheckSum.computeChkSum(recvPack) != recvPack.getTcpH().getTh_sum()) {
            return;
        }
        // 将接收到的包放入接收窗口缓冲，获得缓冲处理结果
        int bufferResult = window.bufferPacker(recvPack);
        System.out.println("Buffering result: " + bufferResult);

        // 将 ACK 字段设为收到包的序号，构造并发送 ACK 报文
        tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
        ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
        tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
        reply(ackPack);

        // 如果接收到了基序号的包，将窗口中可交付的数据包的数据放入交付队列
        if (bufferResult == AckState.BASE.ordinal()) {
            TCP_PACKET packet = window.getPacketToDeliver();
            while (packet != null) {
                dataQueue.add(packet.getTcpS().getData());
                packet = window.getPacketToDeliver();
            }
        }

        System.out.println();
        // 交付数据
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
        /*
         * 0. 信道无差错
         * 1. 只出错
         * 2. 只丢包
         * 3. 只延迟
         * 4. 出错/丢包
         * 5. 出错/延迟
         * 6. 丢包/延迟
         * 7. 出错/丢包/延迟
         */
        tcpH.setTh_eflag((byte) 7);
        // 发送数据报
        client.send(replyPack);
    }
}
