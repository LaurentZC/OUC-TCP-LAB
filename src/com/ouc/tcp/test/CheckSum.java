package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.zip.CRC32;

public class CheckSum {
    CheckSum() {
    }

    /**
     * 计算 TCP 文段校验和
     * 使用 CRC32 算法计算 TCP 报文段中关键字段的校验值
     * 仅对序列号、确认号和 TCP 数据字段进行计算。
     *
     * @param tcpPack 要计算校验和的 TCP 报文段对象
     * @return TCP 报文段的简化校验和，范围为 short
     */
    public static short computeChkSum(TCP_PACKET tcpPack) {
        TCP_HEADER header = tcpPack.getTcpH();

        CRC32 crc32 = new CRC32();
        crc32.update(header.getTh_seq());
        crc32.update(header.getTh_ack());

        for (int data : tcpPack.getTcpS().getData()) {
            crc32.update(data);
        }

        return (short) crc32.getValue();
    }

}
