package ru.stech

import ru.stech.obj.ro.*
import ru.stech.obj.ro.register.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.UUID

class RegisterTransaction(
    val callId: String,
    val datagramChannel: DatagramChannel,
    val sipClientProperties: SipClientProperties
) : Transaction {
    val branch = "z9hG4bK${UUID.randomUUID()}"
    private var cseqNumber = 0
    private val nc = "00000001"

    override fun start() {
        val request = SipRegisterRequest(
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
                sipClientProperties.serverIp),
            callId = callId,
            cSeqOrder = ++cseqNumber,
            expires = 20,
            allow = arrayListOf(
                SipMethod.INVITE, SipMethod.ACK, SipMethod.CANCEL, SipMethod.BYE, SipMethod.NOTIFY, SipMethod.REFER,
                SipMethod.MESSAGE, SipMethod.OPTIONS, SipMethod.INFO, SipMethod.SUBSCRIBE)
        )
        send(request)
    }

    override fun processReceivedBody(branch: String, body: String) {
        val response = body.parseToSipRegisterResponse()
        val cnonce = UUID.randomUUID().toString()
        if (response.status == SipStatus.Unauthorized) {
            val newRegisterRequest = SipRegisterRequest(
                branch = branch,
                maxForwards = 70,
                contactHeader = SipContactHeader(
                    user = sipClientProperties.user,
                    localIp = sipClientProperties.clientIp,
                    localPort = sipClientProperties.clientPort
                ),
                toHeader = SipToHeader(
                    sipClientProperties.user,
                    sipClientProperties.serverIp
                ),
                fromHeader = SipFromHeader(
                    user = sipClientProperties.user,
                    host = sipClientProperties.serverIp
                ),
                callId = callId,
                cSeqOrder = ++cseqNumber,
                authorizationHeader = SipAuthorizationHeader(
                    user = sipClientProperties.user,
                    realm = response.wwwAuthenticateHeader!!.realm,
                    nonce = response.wwwAuthenticateHeader.nonce,
                    serverIp = sipClientProperties.serverIp,
                    response = getResponseHash(nc, cnonce, response.wwwAuthenticateHeader, sipClientProperties),
                    cnonce = cnonce,
                    nc = nc,
                    qop = response.wwwAuthenticateHeader.qop,
                    algorithm = response.wwwAuthenticateHeader.algorithm,
                    opaque = response.wwwAuthenticateHeader.opaque
                ),
                expires = 20,
                allow = arrayListOf(
                    SipMethod.INVITE, SipMethod.ACK, SipMethod.CANCEL, SipMethod.BYE, SipMethod.NOTIFY, SipMethod.REFER,
                    SipMethod.MESSAGE, SipMethod.OPTIONS, SipMethod.INFO, SipMethod.SUBSCRIBE)
            )
            send(newRegisterRequest)
        } else {
            print("register transaction")
        }
    }

    fun send(request: SipRegisterRequest) {
        val buf = ByteBuffer.allocate(2048)
        buf.clear()
        buf.put(request.buildString().toByteArray())
        buf.flip()
        datagramChannel.send(buf, InetSocketAddress(sipClientProperties.serverIp, sipClientProperties.serverPort))
    }
}