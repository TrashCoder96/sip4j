package ru.stech

import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.SipStatus
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.invite.SipInviteRequest
import ru.stech.obj.ro.invite.parseToInviteResponse
import ru.stech.obj.ro.register.SipAuthorizationHeader
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class InviteOperation(
    val remoteUser: String,
    val datagramChannel: DatagramChannel,
    val sipClientProperties: SipClientProperties
): Operation {
    val callId: String = UUID.randomUUID().toString()
    var completed = AtomicBoolean(false)
    private var cseqNumber = 0
    private val nc = "00000001"
    override fun isCompleted(): Boolean {
        return completed.get()
    }

    override fun start() {
        val branch = "z9hG4bK${UUID.randomUUID()}"
        val request = SipInviteRequest(
            branch = branch,
            maxForwards = 70,
            contactHeader = SipContactHeader(sipClientProperties.user, sipClientProperties.clientIp, sipClientProperties.clientPort),
            toHeader = SipToHeader(remoteUser, sipClientProperties.serverIp),
            fromHeader = SipFromHeader(sipClientProperties.user, sipClientProperties.serverIp),
            callId = callId,
            cseqNumber = ++cseqNumber
        )
        send(request)
    }

    override fun processReceivedBody(body: String) {
        val response = body.parseToInviteResponse()
        if (response.status == SipStatus.Unauthorized) {
            val cnonce = UUID.randomUUID().toString()
            val branch = "z9hG4bK${UUID.randomUUID()}"
            val fromTag = extractTagFromReceivedBody(body)
            val newInviteRequest = SipInviteRequest(
                branch = branch,
                contactHeader = SipContactHeader(
                    user = sipClientProperties.user,
                    localIp = sipClientProperties.clientIp,
                    localPort = sipClientProperties.clientPort
                ),
                toHeader = SipToHeader(
                    user = remoteUser,
                    host = sipClientProperties.serverIp
                ),
                fromHeader = SipFromHeader(
                    user = sipClientProperties.user,
                    host = sipClientProperties.serverIp,
                    tag = fromTag
                ),
                maxForwards = 70,
                callId = callId,
                cseqNumber = ++cseqNumber,
                authorizationHeader = SipAuthorizationHeader(
                    user = sipClientProperties.user,
                    realm = response.wwwAuthenticateHeader.realm,
                    nonce = response.wwwAuthenticateHeader.nonce,
                    serverIp = sipClientProperties.serverIp,
                    response = getResponseHash(SipMethod.INVITE, cnonce, nc, response.wwwAuthenticateHeader, sipClientProperties),
                    cnonce = cnonce,
                    nc = nc,
                    qop = response.wwwAuthenticateHeader.qop,
                    algorithm = response.wwwAuthenticateHeader.algorithm,
                    opaque = response.wwwAuthenticateHeader.opaque
                )
            )
            send(newInviteRequest)
        } else if (response.status == SipStatus.OK) {
            completed.set(true)
        } else {
            print("invite transaction")
        }
    }

    fun send(request: SipInviteRequest) {
        val buf = ByteBuffer.allocate(2048)
        buf.clear()
        buf.put(request.buildString().toByteArray())
        buf.flip()
        datagramChannel.send(buf, InetSocketAddress(sipClientProperties.serverIp, sipClientProperties.serverPort))
    }

}