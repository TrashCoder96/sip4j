package ru.stech

import ru.stech.obj.ro.*
import ru.stech.obj.ro.register.*
import java.lang.IllegalArgumentException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class RegisterOperation(
    val datagramChannel: DatagramChannel,
    val sipClientProperties: SipClientProperties
) : Operation {
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
                sipClientProperties.serverIp,
                tag = fromTag
            ),
            callId = callId,
            cSeqOrder = ++cseqNumber,
            expires = 20,
            allow = arrayListOf(
                SipMethod.INVITE, SipMethod.ACK, SipMethod.CANCEL, SipMethod.BYE, SipMethod.NOTIFY, SipMethod.REFER,
                SipMethod.MESSAGE, SipMethod.OPTIONS, SipMethod.INFO, SipMethod.SUBSCRIBE)
        )
        send(request)
    }

    override fun processReceivedBody(body: String) {
        //it's always response body
        val response = body.parseToSipRegisterResponse()
        if (response.status == SipStatus.Unauthorized) {
            val cnonce = UUID.randomUUID().toString()
            val branch = "z9hG4bK${UUID.randomUUID()}"
            val fromTag = extractTagFromReceivedBody(body)
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
                    sipClientProperties.serverIp,
                ),
                fromHeader = SipFromHeader(
                    user = sipClientProperties.user,
                    host = sipClientProperties.serverIp,
                    tag = fromTag
                ),
                callId = callId,
                cSeqOrder = ++cseqNumber,
                authorizationHeader = SipAuthorizationHeader(
                    user = sipClientProperties.user,
                    realm = response.wwwAuthenticateHeader!!.realm,
                    nonce = response.wwwAuthenticateHeader.nonce,
                    serverIp = sipClientProperties.serverIp,
                    response = getResponseHash(SipMethod.REGISTER,
                        cnonce,
                        nc,
                        response.wwwAuthenticateHeader,
                        sipClientProperties),
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
        } else if (response.status == SipStatus.OK) {
            completed.set(true)
        } else {
            throw IllegalArgumentException()
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