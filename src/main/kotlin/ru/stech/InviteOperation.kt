package ru.stech

import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.SipStatus
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.invite.SipInviteRequest
import ru.stech.obj.ro.invite.parseToInviteResponse
import ru.stech.obj.ro.register.SipAuthorizationHeader
import ru.stech.obj.ro.register.WWWAuthenticateHeader
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.*

class InviteOperation(
    val remoteUser: String,
    val callId: String,
    val datagramChannel: DatagramChannel,
    val sipClientProperties: SipClientProperties
): Operation {
    val branch = "z9hG4bK${UUID.randomUUID()}"
    private var cseqNumber = 0
    private val nc = "00000001"
    private var currentWWWWAuthenticateHeader: WWWAuthenticateHeader? = null

    override fun start() {
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
        val cnonce = UUID.randomUUID().toString()
        if (response.status == SipStatus.Unauthorized) {
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
                    host = sipClientProperties.serverIp
                ),
                maxForwards = 70,
                callId = callId,
                cseqNumber = ++cseqNumber,
                authorizationHeader = SipAuthorizationHeader(
                    user = sipClientProperties.user,
                    realm = response.wwwAuthenticateHeader.realm,
                    nonce = response.wwwAuthenticateHeader.nonce,
                    serverIp = sipClientProperties.serverIp,
                    response = getResponseHash(SipMethod.INVITE, nc, cnonce, response.wwwAuthenticateHeader, sipClientProperties),
                    cnonce = cnonce,
                    nc = nc,
                    qop = response.wwwAuthenticateHeader.qop,
                    algorithm = response.wwwAuthenticateHeader.algorithm,
                    opaque = response.wwwAuthenticateHeader.opaque
                )
            )
            send(newInviteRequest)
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