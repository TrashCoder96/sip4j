package ru.stech

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.SipStatus
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.ack.SipAckRequest
import ru.stech.obj.ro.invite.SipInviteRequest
import ru.stech.obj.ro.invite.SipInviteResponse
import ru.stech.obj.ro.invite.parseToInviteResponse
import ru.stech.obj.ro.options.SipOptionsRequest
import ru.stech.obj.ro.options.SipOptionsResponse
import ru.stech.obj.ro.options.parseToOptionsRequest
import ru.stech.obj.ro.register.SipAuthorizationHeader
import ru.stech.obj.ro.register.SipRegisterRequest
import ru.stech.obj.ro.register.SipRegisterResponse
import ru.stech.obj.ro.register.buildString
import ru.stech.obj.ro.register.parseToSipRegisterResponse
import ru.stech.util.extractBranchFromReceivedBody
import ru.stech.util.findIp
import ru.stech.util.getResponseHash
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.UUID

class Client (
    private val user: String,
    private val password: String,
    private val clientPort: Int,
    private val serverIp: String,
    private val serverPort: Int,
    private val dispatcher: CoroutineDispatcher
) {
    private val channel = DatagramChannel.open()
    private val clientIp = findIp()

    private var registerBranch: String = "z9hG4bK${UUID.randomUUID()}"
    private val registerResponseChannel = Channel<SipRegisterResponse>(0)

    private var inviteBranch: String = "z9hG4bK${UUID.randomUUID()}"
    private val inviteResponseChannel = Channel<SipInviteResponse>(0)

    private var optionsBranch: String = "z9hG4bK${UUID.randomUUID()}"
    private val optionsRequestChannel = Channel<SipOptionsRequest>(0)

    private val sipClientProperties: SipClientProperties = SipClientProperties(
        user = user,
        password = password,
        serverIp = serverIp,
        serverPort = serverPort,
        clientIp = clientIp,
        clientPort = clientPort
    )
    private val remoteUser: String = "4090"

    init {
        channel.configureBlocking(false)
        channel.socket().bind(InetSocketAddress(clientPort))
        //loop for options requests from server
        CoroutineScope(dispatcher).launch {
            while (true) {
                val optionsRequest = optionsRequestChannel.receive()
                val optionsResponse = SipOptionsResponse(
                    user = sipClientProperties.user,
                    status = SipStatus.OK,
                    serverIp = sipClientProperties.serverIp,
                    serverPort = sipClientProperties.serverPort,
                    clientIp = sipClientProperties.clientIp,
                    clientPort = sipClientProperties.clientPort,
                    branch = optionsRequest.branch,
                    fromTag = "12345",
                    callId = optionsRequest.callId,
                    cseqNumber = optionsRequest.cseqNumber
                )
                send(optionsResponse.buildString())
            }
        }
    }

    fun send(requestBody: String) {
        val byteBody = requestBody.toByteArray()
        val buf = ByteBuffer.allocate(byteBody.size)
        buf.clear()
        buf.put(byteBody)
        buf.flip()
        channel.send(buf, InetSocketAddress(sipClientProperties.serverIp, sipClientProperties.serverPort))
    }

    fun startListening() {
        val receivedBuf = ByteBuffer.allocate(2048)
        CoroutineScope(dispatcher).launch {
            while (true) {
                receivedBuf.clear()
                if (channel.receive(receivedBuf) != null) {
                    val body = String(receivedBuf.array())
                    sendToAppropriateChannel(body)
                }
            }
        }
        print("started")
    }

    private suspend fun sendToAppropriateChannel(body: String) {
        when (extractBranchFromReceivedBody(body)) {
            registerBranch -> registerResponseChannel.send(body.parseToSipRegisterResponse())
            inviteBranch -> inviteResponseChannel.send(body.parseToInviteResponse())
            else -> {
                val optionsRequest = body.parseToOptionsRequest()
                optionsRequestChannel.send(optionsRequest)
            }
        }
    }

    suspend fun register() {
        val nc = "00000001"
        val callId = UUID.randomUUID().toString()
        val fromTag = UUID.randomUUID().toString()
        val request = SipRegisterRequest(
            branch = registerBranch,
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
            cSeqOrder = 1,
            expires = 20,
            allow = arrayListOf(
                SipMethod.INVITE, SipMethod.ACK, SipMethod.CANCEL, SipMethod.BYE, SipMethod.NOTIFY, SipMethod.REFER,
                SipMethod.MESSAGE, SipMethod.OPTIONS, SipMethod.INFO, SipMethod.SUBSCRIBE)
        )
        send(request.buildString())
        var sipRegisterResponse = registerResponseChannel.receive()
        if (sipRegisterResponse.status == SipStatus.Unauthorized) {
            registerBranch = "z9hG4bK${UUID.randomUUID()}"
            val cnonce = UUID.randomUUID().toString()
            val newRegisterRequest = SipRegisterRequest(
                branch = registerBranch,
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
                cSeqOrder = 2,
                authorizationHeader = SipAuthorizationHeader(
                    user = sipClientProperties.user,
                    realm = sipRegisterResponse.wwwAuthenticateHeader!!.realm,
                    nonce = sipRegisterResponse.wwwAuthenticateHeader!!.nonce,
                    serverIp = sipClientProperties.serverIp,
                    response = getResponseHash(SipMethod.REGISTER,
                        cnonce,
                        nc,
                        sipRegisterResponse.wwwAuthenticateHeader!!,
                        sipClientProperties),
                    cnonce = cnonce,
                    nc = nc,
                    qop = sipRegisterResponse.wwwAuthenticateHeader!!.qop,
                    algorithm = sipRegisterResponse.wwwAuthenticateHeader!!.algorithm,
                    opaque = sipRegisterResponse.wwwAuthenticateHeader!!.opaque
                ),
                expires = 20,
                allow = arrayListOf(
                    SipMethod.INVITE, SipMethod.ACK, SipMethod.CANCEL, SipMethod.BYE, SipMethod.NOTIFY, SipMethod.REFER,
                    SipMethod.MESSAGE, SipMethod.OPTIONS, SipMethod.INFO, SipMethod.SUBSCRIBE)
            )
            send(newRegisterRequest.buildString())
            sipRegisterResponse = registerResponseChannel.receive()
        }
        if (sipRegisterResponse.status == SipStatus.OK) {
            print("Registration is ok")
        } else {
            print("Registration is failed")
        }
    }

    suspend fun makeCall() {
        val nc = "00000001"
        val callId = UUID.randomUUID().toString()
        val fromTag = UUID.randomUUID().toString()
        val inviteRequest = SipInviteRequest(
            branch = inviteBranch,
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
            cseqNumber = 1,
        )
        send(inviteRequest.buildString())
        var sipInviteResponse = inviteResponseChannel.receive()
        while (sipInviteResponse.status == SipStatus.Trying || sipInviteResponse.status == SipStatus.Ringing) {
            sipInviteResponse = inviteResponseChannel.receive()
        }
        ack(inviteBranch, callId)
        if (sipInviteResponse.status == SipStatus.Unauthorized) {
            val cnonce = UUID.randomUUID().toString()
            inviteBranch = "z9hG4bK${UUID.randomUUID()}"
            val newInviteRequest = SipInviteRequest(
                branch = inviteBranch,
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
                cseqNumber = 2,
                authorizationHeader = SipAuthorizationHeader(
                    user = sipClientProperties.user,
                    realm = sipInviteResponse.wwwAuthenticateHeader!!.realm,
                    nonce = sipInviteResponse.wwwAuthenticateHeader!!.nonce,
                    serverIp = sipClientProperties.serverIp,
                    response = getResponseHash(SipMethod.INVITE,
                        cnonce,
                        nc,
                        sipInviteResponse.wwwAuthenticateHeader!!,
                        sipClientProperties),
                    cnonce = cnonce,
                    nc = nc,
                    qop = sipInviteResponse.wwwAuthenticateHeader!!.qop,
                    algorithm = sipInviteResponse.wwwAuthenticateHeader!!.algorithm,
                    opaque = sipInviteResponse.wwwAuthenticateHeader!!.opaque
                ),
            )
            send(newInviteRequest.buildString())
            sipInviteResponse = inviteResponseChannel.receive()
            while (sipInviteResponse.status == SipStatus.Trying || sipInviteResponse.status == SipStatus.Ringing) {
                sipInviteResponse = inviteResponseChannel.receive()
            }
            ack(inviteBranch, callId)
        }
        if (sipInviteResponse.status == SipStatus.OK) {
            print("Invation is ok")
        } else {
            print("Invation is failed")
        }
    }

    private fun ack(branch: String, callId: String) {
        val ackRequest = SipAckRequest(
            clientIp = sipClientProperties.clientIp,
            clientPort = sipClientProperties.clientPort,
            branch = branch,
            toHeader = SipToHeader(
                user = sipClientProperties.user,
                host = sipClientProperties.serverIp,
                tag = branch),
            fromHeader = SipFromHeader(
                user = sipClientProperties.user,
                host = sipClientProperties.serverIp,
                tag = UUID.randomUUID().toString()),
            maxForwards = 70,
            callId = callId,
            cseqNumber = 1
        )
        send(ackRequest.buildString())
    }

}