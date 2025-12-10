# Go-Back-N

Go-Back-N 和 Select Response 相比，Sender 只在窗口左沿放置一个全局的计时器，而不是每个数据包都有一个计时器。
当计时器超时时，重传窗口中的所有数据包。
而 Receiver 也维护一个计时器，当计时器超时时，发回当前最后一个连续收到的包的 ACK，即累积确认。

在实现上，做出如下修改：

- 移除每个数据包的计时器，并在 SenderWindow 类中添加一个计时器
- 添加 GBN_RetransTask 以增加对重发窗口全部包的支持。