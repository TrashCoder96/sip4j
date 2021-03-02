package ru.stech

import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.ack.SipAckRequest
import ru.stech.obj.ro.register.SipRegisterRequest
import ru.stech.obj.ro.register.buildString
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class SipAckOperation(
    val datagramChannel: DatagramChannel,
    val sipClientProperties: SipClientProperties
): Operation {
    val callId: String = UUID.randomUUID().toString()
    var completed = AtomicBoolean(false)
    private var cseqNumber = 0
    private val nc = "00000001"
    private val fromTag = UUID.randomUUID().toString()

    override fun isCompleted(): Boolean {
        return completed.get()
    }

    override fun start() {
        val branch = "z9hG4bK${UUID.randomUUID()}"
        val request = SipAckRequest(
            branch = branch,
            maxForwards = 70,
            contactHeader = SipContactHeader(
                sipClientProperties.user,
                sipClientProperties.clientIp,
                sipClientProperties.clientPort),
            toHeader = SipToHeader(
                sipClientProperties.user,
                sipClientProperties.serverIp),
            fromHeader = SipFromHeader(
                sipClientProperties.user,
                sipClientProperties.serverIp,
                tag = fromTag
            ),
            callId = callId,
            cseqNumber = ++cseqNumber
        )
        send(request)
    }

    fun send(request: SipAckRequest) {
        val buf = ByteBuffer.allocate(2048)
        buf.clear()
        buf.put(request.buildString().toByteArray())
        buf.flip()
        datagramChannel.send(buf, InetSocketAddress(sipClientProperties.serverIp, sipClientProperties.serverPort))
    }

    override fun processReceivedBody(body: String) {
        TODO("Not yet implemented")
    }

}