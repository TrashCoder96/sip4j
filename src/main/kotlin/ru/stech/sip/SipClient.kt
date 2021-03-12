package ru.stech.sip

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel

class SipClient(
    val serverHost: String,
    val serverPort: Int,
    private var workerGroup: EpollEventLoopGroup? = null
) {
    private var senderChannel: Channel? = null

    fun start() {
        val bootstrap = Bootstrap()
            .group(workerGroup)
            .channel(EpollServerSocketChannel::class.java)
        senderChannel = bootstrap.bind(serverHost, serverPort).syncUninterruptibly().channel()
    }

    fun stop() {
        senderChannel?.close()
        senderChannel?.closeFuture()?.syncUninterruptibly()
    }

    fun send(data: ByteArray) {
        senderChannel?.writeAndFlush(data)
    }

}