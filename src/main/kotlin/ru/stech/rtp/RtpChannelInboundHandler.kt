package ru.stech.rtp

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.DatagramPacket

class RtpChannelInboundHandler(): SimpleChannelInboundHandler<DatagramPacket>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: DatagramPacket) {
        TODO("Not yet implemented")
    }
}