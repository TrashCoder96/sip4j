package ru.stech.sip

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

class SipReceiver(): SimpleChannelInboundHandler<ByteArray>() {

    override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteArray) {
        TODO("Not yet implemented")
    }
}