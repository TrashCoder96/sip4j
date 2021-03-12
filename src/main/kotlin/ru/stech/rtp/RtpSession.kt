package ru.stech.rtp

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.epoll.EpollDatagramChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.ChannelInitializer
import kotlinx.coroutines.CoroutineDispatcher
import java.lang.Exception

class RtpSession(
    val localPort: Int,
    val remoteHost: String,
    val remotePort: Int,
    private val coroutineDispatcher: CoroutineDispatcher,
    private val eventLoopGroup: EpollEventLoopGroup
) {
    private var future: ChannelFuture? = null

    /**
     * Start listening responses from remote rtp-server
     */
    fun start() {
        val clientBootstrap = Bootstrap()
        clientBootstrap
            .channel(EpollDatagramChannel::class.java)
            .group(eventLoopGroup)
            .handler(object : ChannelInitializer<EpollDatagramChannel>() {
                @Throws(Exception::class)
                override fun initChannel(ch: EpollDatagramChannel) {
                    ch.pipeline().addLast(RtpChannelInboundHandler())
                }
            })
        future =  clientBootstrap.bind(localPort).sync()
    }


    /**
     * Stop listening responses from rtp-server
     */
    fun stop() {
        if (future != null) {
            future?.channel()?.close()
        } else {
            throw RtpException("Rtp session is not started")
        }
    }
}