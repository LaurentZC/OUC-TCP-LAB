# TCP-Tahoe

TCP Tahoe 用 3 个方法来拥塞控制，分别是慢开始、拥塞避免和快重传。

1. 慢开始：由 cwnd 和 ssthresh 两个变量控制。
    - cwnd 表示当前窗口大小
    - ssthresh 表示慢开始阈值
    - 在慢开始阶段，cwnd 每收到一个 ACK 就加 1，直到 cwnd 达到 ssthresh，然后进入拥塞避免阶段

2. 拥塞避免：cwnd 按照 1/cwnd 的比例增加（每一个 RTT 加 1），进入加法增大阶段
3. 快重传：当连续收到 3 个相同的 ACK 时，ssthresh 减半，cwnd 重置为 1，立即重传丢失的包。