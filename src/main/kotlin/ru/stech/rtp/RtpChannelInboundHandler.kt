package ru.stech.rtp

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.net.DatagramPacket

class RtpChannelInboundHandler: SimpleChannelInboundHandler<DatagramPacket>() {
    override fun channelRead0(ctx: ChannelHandlerContext, datagramPacket: DatagramPacket) {
        println("fuck")
    }
}