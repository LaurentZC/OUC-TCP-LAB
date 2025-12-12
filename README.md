# TCP-Reno

Reno 版本在 Tahoe 版本上增加了快重传和快恢复

- 快重传：接收方收到三个重复 ACK 时，立刻重发丢失的包
- 快恢复：快重传之后 cwnd 设置为 ssthresh，实现乘法减小