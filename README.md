# TCP-Reno

Reno 版本在 Tahoe 版本上增加了快恢复

- 快恢复：快重传之后 cwnd 设置为 ssthresh，实现乘法减小