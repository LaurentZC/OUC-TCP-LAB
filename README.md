# RDT 2.x 可靠数据传输协议

## RDT 2.1

接收方按次序接收数据包，如果接收到的数据包校验和正确且序号与预期一致，则发送确认（ACK）并将数据交付给上层；
否则，发送否认确认（NAK）请求重传。

## RDT 2.2

RDT 2.2 是对 RDT 2.1 的改进版本，使用前一个包的序号来表达否认确认（NAK），并在 ACK 出错时通过重传上一个数据包来确保数据的可靠传输。

注意到数据包中的 `seq` 表示数据包的字节流序号，而接收方的 `sequence` 是一个递增的值（即认为是数据包的序号），
因此可以用发送方的 `seq` 来得到接收方的 `sequence`，具体而言：

$$
\text{seq} = \text{dataIndex} \times \text{dataLength} + 1 \leftrightarrow \text{dataIndex} = \frac{\text{seq} - 1}{\text{dataLength}}
$$

这样，需要表达否定时，只需要将上一个正确接收的包的 `dataIndex` 通过上述公式转换为 `seq` 即可。