package ru.stech

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.SipStatus
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader
import ru.stech.obj.ro.ack.SipAckRequest
import ru.stech.obj.ro.bye.SipByeRequest
import ru.stech.obj.ro.bye.SipByeResponse
import ru.stech.obj.ro.bye.parseToByeRequest
import ru.stech.obj.ro.invite.SipInviteRequest
import ru.stech.obj.ro.invite.SipInviteResponse
import ru.stech.obj.ro.invite.parseToInviteResponse
import ru.stech.obj.ro.options.SipOptionsRequest
import ru.stech.obj.ro.options.SipOptionsResponse
import ru.stech.obj.ro.options.parseToOptionsRequest
import ru.stech.obj.ro.register.SipAuthorizationHeader
import ru.stech.obj.ro.register.SipRegisterRequest
import ru.stech.obj.ro.register.SipRegisterResponse
import ru.stech.obj.ro.register.parseToSipRegisterResponse
import ru.stech.util.extractBranchFromReceivedBody
import ru.stech.util.findIp
import ru.stech.util.findMethod
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
    private val rtpChannel = DatagramChannel.open()
    private val clientIp = findIp()

    private val callId = UUID.randomUUID().toString()
    private var registerCallId = UUID.randomUUID().toString()
    private var registerNonce: String? = null
    private var registerRealm: String? = null
    private var registerNc = 0
    private var registerQop: String? = null
    private var registerAlgorithm: String? = null
    private var registerOpaque: String? = null

    private var registerBranch = "z9hG4bK${UUID.randomUUID()}"
    private val registerResponseChannel = Channel<SipRegisterResponse>(0)

    private var inviteBranch = "z9hG4bK${UUID.randomUUID()}"
    private val inviteResponseChannel = Channel<SipInviteResponse>(0)

    private val optionsRequestChannel = Channel<SipOptionsRequest>(0)

    private val byeRequestChannel = Channel<SipByeRequest>(0)

    private val sipClientProperties: SipClientProperties = SipClientProperties(
        user = user,
        password = password,
        serverIp = serverIp,
        serverPort = serverPort,
        clientIp = clientIp,
        clientPort = clientPort,
        rtpPort = 30040
    )
    private val remoteUser: String = "4090"

    init {
        channel.configureBlocking(false)
        channel.socket().bind(InetSocketAddress(clientPort))
        //loop for options requests from server
        CoroutineScope(dispatcher).launch {
            while (true) {
                select<Unit> {
                    optionsRequestChannel.onReceive {
                        val optionsResponse = SipOptionsResponse(
                            status = SipStatus.OK,
                            viaHeader = SipViaHeader(
                                host = sipClientProperties.clientIp,
                                port = sipClientProperties.clientPort,
                                hostParams = mapOf(
                                    "rport" to "${sipClientProperties.serverPort}",
                                    "branch" to it.viaHeader.hostParams["branch"]!!
                                )
                            ),
                            fromHeader = SipFromHeader(
                                user = it.fromHeader.user,
                                host = it.fromHeader.host,
                                fromParamsMap = mapOf(
                                    "tag" to it.fromHeader.fromParamsMap["tag"]!!
                                )
                            ),
                            toHeader = SipToHeader(
                                user = sipClientProperties.user,
                                host = sipClientProperties.clientIp,
                                hostParamsMap = mapOf(
                                    "rinstance" to it.toHeader.hostParamsMap["rinstance"]!!
                                )
                            ),
                            cSeqHeader = CSeqHeader(
                                cSeqNumber = it.cSeqHeader.cSeqNumber,
                                method = SipMethod.OPTIONS
                            ),
                            callIdHeader = CallIdHeader(
                                callId = it.callIdHeader.callId
                            )
                        )
                        send(optionsResponse.buildString())
                    }
                    byeRequestChannel.onReceive {
                        val byeResponse = SipByeResponse(
                            status = SipStatus.OK,
                            viaHeader = SipViaHeader(

                            ),
                            serverIp = sipClientProperties.serverIp,
                            serverPort = sipClientProperties.serverPort,
                            branch = it.branch,
                            callId = it.callId,
                            contactHeader = SipContactHeader(
                                user = sipClientProperties.user,
                                localIp = sipClientProperties.clientIp,
                                localPort = sipClientProperties.clientPort
                            ),
                            toHeader = SipToHeader(
                                user = sipClientProperties.user,
                                host = sipClientProperties.serverIp
                            ),
                            fromHeader = it.fromHeader,
                            cseqNumber = it.cseqNumber
                        )
                        send(byeResponse.buildString())
                    }
                }
            }
        }
        rtpChannel.configureBlocking(false)
        rtpChannel.socket().bind(InetSocketAddress(sipClientProperties.rtpPort))
        //loop for rtp stream from server
        CoroutineScope(dispatcher).launch {
            while (true) {
                val receivedBuf = ByteBuffer.allocate(2048)
                receivedBuf.clear()
                if (rtpChannel.receive(receivedBuf) != null) {
                    print("pcm\n")
                }
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
                val method = findMethod(body)
                when (SipMethod.valueOf(method)) {
                    SipMethod.OPTIONS -> optionsRequestChannel.send(body.parseToOptionsRequest())
                    SipMethod.BYE -> byeRequestChannel.send(body.parseToByeRequest())
                    else -> {

                    }
                }
            }
        }
    }

    suspend fun register() {
        val fromTag = UUID.randomUUID().toString()
        val request = SipRegisterRequest(
            viaHeader = SipViaHeader(
                host = sipClientProperties.clientIp,
                port = sipClientProperties.clientPort,
                branch = registerBranch
            ),
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
            callIdHeader = CallIdHeader(
                callId = registerCallId
            ),
            cSeqHeader = CSeqHeader(
                cSeqNumber = 1,
                method = SipMethod.REGISTER
            ),
            expires = 20,
            allow = arrayListOf(
                SipMethod.INVITE, SipMethod.ACK, SipMethod.CANCEL, SipMethod.BYE, SipMethod.NOTIFY, SipMethod.REFER,
                SipMethod.MESSAGE, SipMethod.OPTIONS, SipMethod.INFO, SipMethod.SUBSCRIBE)
        )
        send(request.buildString())
        var sipRegisterResponse = registerResponseChannel.receive()
        if (sipRegisterResponse.status == SipStatus.Unauthorized) {
            val nc = String.format("%08d", ++registerNc)
            val cnonce = UUID.randomUUID().toString()
            registerBranch = "z9hG4bK${UUID.randomUUID()}"
            registerNonce = sipRegisterResponse.wwwAuthenticateHeader!!.nonce
            registerRealm = sipRegisterResponse.wwwAuthenticateHeader!!.realm
            registerQop = sipRegisterResponse.wwwAuthenticateHeader!!.qop
            registerAlgorithm = sipRegisterResponse.wwwAuthenticateHeader!!.algorithm
            registerOpaque = sipRegisterResponse.wwwAuthenticateHeader!!.opaque
            val newRegisterRequest = SipRegisterRequest(
                viaHeader = SipViaHeader(
                    host = sipClientProperties.clientIp,
                    port = sipClientProperties.clientPort,
                    branch = registerBranch
                ),
                contactHeader = SipContactHeader(
                    user = sipClientProperties.user,
                    ip = sipClientProperties.clientIp,
                    port = sipClientProperties.clientPort
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
                maxForwards = 70,
                callIdHeader = CallIdHeader(
                    callId = registerCallId
                ),
                cSeqHeader = CSeqHeader(
                    cSeqNumber = 2,
                    method = SipMethod.REGISTER
                ),
                authorizationHeader = SipAuthorizationHeader(
                    user = sipClientProperties.user,
                    realm = registerRealm!!,
                    nonce = registerNonce!!,
                    serverIp = sipClientProperties.serverIp,
                    response = getResponseHash(sipClientProperties.user,
                        registerRealm!!,
                        sipClientProperties.password,
                        SipMethod.REGISTER,
                        sipClientProperties.serverIp,
                        registerNonce!!,
                        nc,
                        cnonce,
                        registerQop!!),
                    cnonce = cnonce,
                    nc = nc,
                    qop = registerQop!!,
                    algorithm = registerAlgorithm!!,
                    opaque = registerOpaque!!
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

    suspend fun unregister() {
        registerBranch = "z9hG4bK${UUID.randomUUID()}"
        val nc = String.format("%08d", ++registerNc)
        val cnonce = UUID.randomUUID().toString()
        val fromTag = UUID.randomUUID().toString()
        val unregisterRequest = SipRegisterRequest(
            viaHeader = SipViaHeader(
                host = sipClientProperties.clientIp,
                port = sipClientProperties.clientPort,
                branch = registerBranch
            ),
            contactHeader = SipContactHeader(
                user = sipClientProperties.user,
                ip = sipClientProperties.clientIp,
                port = sipClientProperties.clientPort
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
            maxForwards = 70,
            callIdHeader = CallIdHeader(
                callId = registerCallId
            ),
            cSeqHeader = CSeqHeader(
                cSeqNumber = 3,
                method = SipMethod.REGISTER
            ),
            authorizationHeader = SipAuthorizationHeader(
                user = sipClientProperties.user,
                realm = registerRealm!!,
                nonce = registerNonce!!,
                serverIp = sipClientProperties.serverIp,
                response = getResponseHash(sipClientProperties.user,
                    registerRealm!!,
                    sipClientProperties.password,
                    SipMethod.REGISTER,
                    sipClientProperties.serverIp,
                    registerNonce!!,
                    nc,
                    cnonce,
                    registerQop!!),
                cnonce = cnonce,
                nc = nc,
                qop = registerQop!!,
                algorithm = registerAlgorithm!!,
                opaque = registerOpaque!!
            ),
            expires = 20,
            allow = arrayListOf(
                SipMethod.INVITE, SipMethod.ACK, SipMethod.CANCEL, SipMethod.BYE, SipMethod.NOTIFY, SipMethod.REFER,
                SipMethod.MESSAGE, SipMethod.OPTIONS, SipMethod.INFO, SipMethod.SUBSCRIBE)
        )
        send(unregisterRequest.buildString())
        var sipRegisterResponse = registerResponseChannel.receive()
        if (sipRegisterResponse.status == SipStatus.Unauthorized) {
            registerBranch = UUID.randomUUID().toString()
            val newUnregisterRequest = SipRegisterRequest(
                viaHeader = SipViaHeader(
                    host = sipClientProperties.clientIp,
                    port = sipClientProperties.clientPort,
                    branch = registerBranch
                ),
                contactHeader = SipContactHeader(
                    user = sipClientProperties.user,
                    ip = sipClientProperties.clientIp,
                    port = sipClientProperties.clientPort
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
                maxForwards = 70,
                callIdHeader = CallIdHeader(
                    callId = registerCallId
                ),
                cSeqHeader = CSeqHeader(
                    cSeqNumber = 4,
                    method = SipMethod.REGISTER
                ),
                authorizationHeader = SipAuthorizationHeader(
                    user = sipClientProperties.user,
                    realm = registerRealm!!,
                    nonce = registerNonce!!,
                    serverIp = sipClientProperties.serverIp,
                    response = getResponseHash(sipClientProperties.user,
                        registerRealm!!,
                        sipClientProperties.password,
                        SipMethod.REGISTER,
                        sipClientProperties.serverIp,
                        registerNonce!!,
                        nc,
                        cnonce,
                        registerQop!!),
                    cnonce = cnonce,
                    nc = nc,
                    qop = registerQop!!,
                    algorithm = registerAlgorithm!!,
                    opaque = registerOpaque!!
                ),
                expires = 20,
                allow = arrayListOf(
                    SipMethod.INVITE, SipMethod.ACK, SipMethod.CANCEL, SipMethod.BYE, SipMethod.NOTIFY, SipMethod.REFER,
                    SipMethod.MESSAGE, SipMethod.OPTIONS, SipMethod.INFO, SipMethod.SUBSCRIBE)
            )
            send(newUnregisterRequest.buildString())
            sipRegisterResponse = registerResponseChannel.receive()
        }
        if (sipRegisterResponse.status == SipStatus.OK) {
            print("Unregistration is ok")
        } else {
            print("Unregistration is failed")
        }
    }

    suspend fun stopCall() {

    }

    suspend fun startCall() {
        val nc = "00000001"
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
            rtpPort = sipClientProperties.rtpPort
        )
        send(inviteRequest.buildString())
        var sipInviteResponse = inviteResponseChannel.receive()
        while (sipInviteResponse.status == SipStatus.Trying || sipInviteResponse.status == SipStatus.Ringing) {
            sipInviteResponse = inviteResponseChannel.receive()
        }
        ack(inviteBranch)
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
                    response = getResponseHash(sipClientProperties.user,
                        sipInviteResponse.wwwAuthenticateHeader!!.realm,
                        sipClientProperties.password,
                        SipMethod.INVITE,
                        sipClientProperties.serverIp,
                        sipInviteResponse.wwwAuthenticateHeader!!.nonce,
                        nc,
                        cnonce,
                        sipInviteResponse.wwwAuthenticateHeader!!.qop),
                    cnonce = cnonce,
                    nc = nc,
                    qop = sipInviteResponse.wwwAuthenticateHeader!!.qop,
                    algorithm = sipInviteResponse.wwwAuthenticateHeader!!.algorithm,
                    opaque = sipInviteResponse.wwwAuthenticateHeader!!.opaque
                ),
                rtpPort = sipClientProperties.rtpPort
            )
            send(newInviteRequest.buildString())
            sipInviteResponse = inviteResponseChannel.receive()
            while (sipInviteResponse.status == SipStatus.Trying || sipInviteResponse.status == SipStatus.Ringing) {
                sipInviteResponse = inviteResponseChannel.receive()
            }
            ack(inviteBranch)
        }
        if (sipInviteResponse.status == SipStatus.OK) {
            print("Invation is ok")
        } else {
            print("Invation is failed")
        }
    }

    private fun ack(branch: String) {
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