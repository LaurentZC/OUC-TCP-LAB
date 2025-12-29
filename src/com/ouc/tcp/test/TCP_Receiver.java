/**
 * 2.1: ACK/NACK
 * Feng Hong; 2015-12-09
 */

package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.test.windows.AckState;
import com.ouc.tcp.test.windows.ReceiverWindow;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TimerTask;

public class TCP_Receiver extends TCP_Receiver_ADT {
    TCP_PACKET ackPack;
    // 接收者窗口
    private final ReceiverWindow window = new ReceiverWindow(16);
    private UDT_Timer timer = new UDT_Timer();

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
        // 校验和检查：若校验失败则丢弃该报文段
        if (CheckSum.computeChkSum(recvPack) != recvPack.getTcpH().getTh_sum()) {
            System.out.println("Checksum error, packet discarded.");
            return;
        }

        // 将接收到的包放入接收窗口缓冲，获得缓冲处理结果
        AckState bufferResult = window.bufferPacket(recvPack);
        System.out.println("Buffering result: 包 " + recvPack.getTcpH().getTh_seq() + " " + bufferResult);

        // 如果是窗口左边界的数据包，计时 500ms 等到其他包
        if (bufferResult == AckState.BASE) {
            // 处理所有可交付的数据包
            TCP_PACKET packet = window.getPacketToDeliver();
            while (packet != null) {
                // 提取数据并放入交付队列
                dataQueue.add(packet.getTcpS().getData());
                // 准备 ACK 报文段
                tcpH.setTh_ack(packet.getTcpH().getTh_seq());
                ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
                tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
                // 获取下一个可交付的数据包
                packet = window.getPacketToDeliver();
            }

            // 设置延迟 ACK
            timer.cancel();
            timer = new UDT_Timer();
            timer.schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            reply(ackPack);
                        }
                    }, 500
            );
        }
        // 对于无序到达的数据包，立即发送 ACK
        else if (bufferResult != AckState.ORDERED) {
            reply(ackPack);
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
