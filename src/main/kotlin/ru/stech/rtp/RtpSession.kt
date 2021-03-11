package ru.stech.rtp

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.epoll.EpollDatagramChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RtpSession(
    val localPort: Int,
    val remoteRtpHost: String,
    val remoteRtpPort: Int,
    private val dispatcher: CoroutineDispatcher,
    private val workerGroup: EpollEventLoopGroup
) {
    fun start() {
        CoroutineScope(dispatcher).launch {
            val bootstrap = Bootstrap()
            bootstrap
                .channel(EpollDatagramChannel::class.java)
                .group(workerGroup)
                .handler(object : ChannelInitializer<EpollDatagramChannel>() {
                    override fun initChannel(ch: EpollDatagramChannel) {
                        ch.pipeline().addLast(RtpChannelInboundHandler())
                    }
                })
            bootstrap.bind(localPort)
        }
    }
}