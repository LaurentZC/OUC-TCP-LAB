package com.ouc.tcp.test.windows;

import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.test.elements.ReceiverElement;
import com.ouc.tcp.test.elements.ReceiverElementFlag;

/**
 * 接收方滑动窗口
 * 管理已接收但未交付、已接收并有序、已交付的TCP数据包
 */
public class ReceiverWindow extends SlidingWindow<ReceiverElement> {

    /**
     * 构造函数
     *
     * @param size 接收窗口大小
     */
    public ReceiverWindow(int size) {
        super(size);
    }

    @Override
    protected ReceiverElement[] createWindowArray(int size) {
        // 创建接收窗口元素数组
        return new ReceiverElement[size];
    }

    @Override
    protected ReceiverElement createElement(int index) {
        // 创建接收窗口元素实例
        return new ReceiverElement();
    }

    /**
     * 获取下一个可交付的数据包
     * 当窗口基序号对应的数据包已缓冲时，将其取出并滑动窗口
     *
     * @return 下一个可交付的 TCP 数据包，如果基序号数据包未缓冲则返回 null
     */
    public TCP_PACKET getPacketToDeliver() {
        // 检查窗口基序号对应的数据包是否已缓冲
        if (!window[getIdx(base)].isBuffered()) {
            return null;  // 基序号数据包未就绪，无法交付
        }

        // 获取基序号数据包
        TCP_PACKET packet = window[getIdx(base)].getTcpPacket();
        // 重置窗口元素
        window[getIdx(base)].reset();
        // 滑动窗口基序号
        base++;

        return packet;  // 返回可交付的数据包
    }

    /**
     * 缓冲接收到的数据包
     * 根据数据包序号决定处理方式
     *
     * @param packet 接收到的 TCP 数据包
     * @return 数据包到达状态对应的枚举值
     */
    public int bufferPacker(TCP_PACKET packet) {
        // 计算数据包序号（从 1 开始的逻辑序号转换为窗口内的相对序号）
        int seq = (packet.getTcpH().getTh_seq() - 1) / packet.getTcpS().getData().length;

        // 情况 1：数据包序号超出接收窗口范围
        if (seq >= base + size) {
            // 数据包超出窗口右边界，可能是未来数据包
            return AckState.DISORDERED.ordinal();  // 返回无序到达状态
        }

        // 情况2：数据包序号小于窗口基序号
        if (seq < base) {
            // 数据包序号在窗口左边界之前，可能是重复或过时数据包
            return AckState.DUPLICATE.ordinal();  // 返回重复到达状态
        }

        // 情况 3&4：数据包序号在窗口范围内
        // 将数据包缓冲到窗口中的对应位置
        window[getIdx(seq)].setElement(packet, ReceiverElementFlag.BUFFERED.ordinal());

        // 情况4：数据包序号正好等于窗口基序号
        if (seq == base) {
            // 这是期望的下一个数据包，可以尝试交付
            return AckState.BASE.ordinal();  // 返回基准确认状态
        }

        // 情况3：数据包在窗口内但不是基序号
        return AckState.ORDERED.ordinal();  // 返回有序到达状态
    }
}