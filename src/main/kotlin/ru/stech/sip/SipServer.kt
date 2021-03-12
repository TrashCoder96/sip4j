package ru.stech.sip

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.ChannelInitializer
import io.netty.channel.epoll.EpollDatagramChannel
import java.lang.Exception


class SipServer(private val port: Int,
                private var bossGroup: EpollEventLoopGroup? = null,
                private var workerGroup: EpollEventLoopGroup? = null
) {
    private var receiverChannel: Channel? = null

    /**
     * Start sip server
     */
    fun start() {
        try {
            bossGroup = EpollEventLoopGroup(1)
            workerGroup = EpollEventLoopGroup(2)
            val bootstrap = ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(EpollServerSocketChannel::class.java)
                .handler(object : ChannelInitializer<EpollDatagramChannel>() {
                    @Throws(Exception::class)
                    override fun initChannel(ch: EpollDatagramChannel) {
                        ch.pipeline().addLast(SipReceiver())
                    }
                })
            receiverChannel = bootstrap.bind(port)
                .syncUninterruptibly().channel()
        } catch (e: Exception) {

        } finally {
            stop()
        }
    }

    /**
     * Stop sip server
     */
    fun stop() {
        receiverChannel?.close()
        receiverChannel?.closeFuture()?.syncUninterruptibly()
    }

}