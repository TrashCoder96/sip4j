package ru.stech

import ru.stech.obj.ro.SipStatus
import ru.stech.obj.ro.options.SipOptionsResponse
import ru.stech.obj.ro.options.parseToOptionsRequest
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class OptionsServerTransaction(
    val callId: String,
    val datagramChannel: DatagramChannel,
    val sipClientProperties: SipClientProperties
): ServerTransaction {

    override fun askRequest(branch: String, request: String) {
        val optionsRequest = request.parseToOptionsRequest()
        val optionsResponse = SipOptionsResponse(
            user = sipClientProperties.user,
            status = SipStatus.OK,
            serverIp = sipClientProperties.serverIp,
            serverPort = sipClientProperties.serverPort,
            clientIp = sipClientProperties.clientIp,
            clientPort = sipClientProperties.clientPort,
            branch = branch,
            fromTag = "12345",
            callId = callId,
            cseqNumber = optionsRequest.cseqNumber
        )
        send(optionsResponse)
    }

    fun send(response: SipOptionsResponse) {
        val buf = ByteBuffer.allocate(2048)
        buf.clear()
        buf.put(response.buildString().toByteArray())
        buf.flip()
        datagramChannel.send(buf, InetSocketAddress(sipClientProperties.serverIp, sipClientProperties.serverPort))
    }

}