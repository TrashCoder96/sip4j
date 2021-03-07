package ru.stech

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipAuthException
import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.SipRequestURIHeader
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
import ru.stech.util.randomString
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.*
import javax.naming.AuthenticationException

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

    private var rinstance = randomString(16)
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
    private val remoteUser = "4090"

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
                                host = sipClientProperties.serverIp,
                                port = sipClientProperties.serverPort,
                                hostParams = linkedMapOf(
                                    "rport" to "${sipClientProperties.serverPort}",
                                    "branch" to it.viaHeader.hostParams["branch"]!!
                                )
                            ),
                            contactHeader = SipContactHeader(
                                user = sipClientProperties.user,
                                host = sipClientProperties.clientIp,
                                port = sipClientProperties.clientPort
                            ),
                            fromHeader = SipFromHeader(
                                user = it.fromHeader.user,
                                host = it.fromHeader.host,
                                fromParamsMap = linkedMapOf(
                                    "tag" to it.fromHeader.fromParamsMap["tag"]!!
                                )
                            ),
                            toHeader = SipToHeader(
                                user = sipClientProperties.user,
                                host = sipClientProperties.clientIp,
                                hostParamsMap = linkedMapOf(
                                    "rinstance" to it.toHeader.hostParamsMap["rinstance"]!!,
                                ),
                                toParamsMap = linkedMapOf(
                                    "tag" to randomString(8)
                                )
                            ),
                            cSeqHeader = it.cSeqHeader,
                            callIdHeader = it.callIdHeader
                        )
                        send(optionsResponse.buildString())
                    }
                    byeRequestChannel.onReceive {
                        val byeResponse = SipByeResponse(
                            status = SipStatus.OK,
                            viaHeader = SipViaHeader(
                                host = it.viaHeader.host,
                                port = it.viaHeader.port,
                                hostParams = linkedMapOf(
                                    "rport" to "${sipClientProperties.serverPort}",
                                    "branch" to it.viaHeader.hostParams["branch"]!!
                                )
                            ),
                            fromHeader = it.fromHeader,
                            toHeader = it.toHeader,
                            contactHeader = SipContactHeader(
                                user = sipClientProperties.user,
                                host = sipClientProperties.clientIp,
                                port = sipClientProperties.clientPort,
                                hostParamsMap = linkedMapOf(
                                    "transport" to "UDP"
                                )
                            ),
                            cSeqHeader = it.cSeqHeader,
                            callIdHeader = it.callIdHeader
                        )
                        send(byeResponse.buildString())
                    }
                }
            }
        }
        rtpChannel.configureBlocking(false)
        rtpChannel.socket().bind(InetSocketAddress(sipClientProperties.rtpPort))
        //loop for rtp stream from server
        val receivedBuf = ByteBuffer.allocate(2048)
        CoroutineScope(dispatcher).launch {
            while (true) {
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
        println(requestBody)
        channel.send(buf, InetSocketAddress(sipClientProperties.serverIp, sipClientProperties.serverPort))
    }

    fun startListening() {
        val receivedBuf = ByteBuffer.allocate(2048)
        CoroutineScope(dispatcher).launch {
            while (true) {
                receivedBuf.clear()
                if (channel.receive(receivedBuf) != null) {
                    val body = String(Arrays.copyOfRange(receivedBuf.array(), 0, receivedBuf.position()))
                    println("1--------------------")
                    println(body)
                    println("2--------------------")
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
        val fromTag = randomString(8)
        val request = SipRegisterRequest(
            requestURIHeader = SipRequestURIHeader(
                method = SipMethod.REGISTER,
                host = sipClientProperties.serverIp,
                hostParamsMap = linkedMapOf(
                    "transport" to "UDP"
                )
            ),
            viaHeader = SipViaHeader(
                host = sipClientProperties.clientIp,
                port = sipClientProperties.clientPort,
                hostParams = linkedMapOf(
                    "branch" to registerBranch,
                    "rport" to ""
                )
            ),
            toHeader = SipToHeader(
                user = sipClientProperties.user,
                host = sipClientProperties.serverIp,
                hostParamsMap = linkedMapOf(
                    "transport" to "UDP"
                )
            ),
            fromHeader = SipFromHeader(
                user = sipClientProperties.user,
                host = sipClientProperties.serverIp,
                hostParamsMap = linkedMapOf(
                    "transport" to "UDP"
                ),
                fromParamsMap = linkedMapOf(
                    "tag" to fromTag
                )
            ),
            contactHeader = SipContactHeader(
                sipClientProperties.user,
                sipClientProperties.clientIp,
                sipClientProperties.clientPort,
                hostParamsMap = linkedMapOf(
                    "rinstance" to rinstance,
                    "transport" to "UDP"
                )
            ),
            maxForwards = 70,
            callIdHeader = CallIdHeader(
                callId = registerCallId
            ),
            cSeqHeader = CSeqHeader(
                cSeqNumber = 1,
                method = SipMethod.REGISTER
            ),
            expires = 60,
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
                requestURIHeader = SipRequestURIHeader(
                    method = SipMethod.REGISTER,
                    host = sipClientProperties.serverIp,
                    hostParamsMap = linkedMapOf(
                        "transport" to "UDP"
                    )
                ),
                viaHeader = SipViaHeader(
                    host = sipClientProperties.clientIp,
                    port = sipClientProperties.clientPort,
                    hostParams = linkedMapOf(
                        "rport" to "",
                        "branch" to registerBranch
                    )
                ),
                contactHeader = SipContactHeader(
                    user = sipClientProperties.user,
                    host = sipClientProperties.clientIp,
                    port = sipClientProperties.clientPort,
                    hostParamsMap = linkedMapOf(
                        "rinstance" to rinstance,
                        "transport" to "UDP"
                    )
                ),
                toHeader = SipToHeader(
                    user = sipClientProperties.user,
                    host = sipClientProperties.serverIp,
                    hostParamsMap = linkedMapOf(
                        "transport" to "UDP"
                    )
                ),
                fromHeader = SipFromHeader(
                    user = sipClientProperties.user,
                    host = sipClientProperties.serverIp,
                    hostParamsMap = linkedMapOf(
                        "transport" to "UDP"
                    ),
                    fromParamsMap = linkedMapOf(
                        "tag" to fromTag
                    )
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
                expires = 60,
                allow = arrayListOf(
                    SipMethod.INVITE, SipMethod.ACK, SipMethod.CANCEL, SipMethod.BYE, SipMethod.NOTIFY, SipMethod.REFER,
                    SipMethod.MESSAGE, SipMethod.OPTIONS, SipMethod.INFO, SipMethod.SUBSCRIBE)
            )
            send(newRegisterRequest.buildString())
            sipRegisterResponse = registerResponseChannel.receive()
        }
        if (sipRegisterResponse.status != SipStatus.OK) {
            throw SipAuthException()
        }
    }

    suspend fun unregister() {
        registerBranch = "z9hG4bK${UUID.randomUUID()}"
        val nc = String.format("%08d", ++registerNc)
        val cnonce = UUID.randomUUID().toString()
        val fromTag = randomString(8)
        val unregisterRequest = SipRegisterRequest(
            requestURIHeader = SipRequestURIHeader(
                method = SipMethod.REGISTER,
                host = sipClientProperties.serverIp,
                hostParamsMap = linkedMapOf(
                    "transport" to "UDP"
                )
            ),
            viaHeader = SipViaHeader(
                host = sipClientProperties.clientIp,
                port = sipClientProperties.clientPort,
                hostParams = linkedMapOf(
                    "rport" to "",
                    "branch" to registerBranch
                )
            ),
            contactHeader = SipContactHeader(
                user = sipClientProperties.user,
                host = sipClientProperties.clientIp,
                port = sipClientProperties.clientPort,
                hostParamsMap = linkedMapOf(
                    "rinstance" to rinstance
                ),
                contactParamsMap = linkedMapOf(
                    "expires" to "0"
                )
            ),
            toHeader = SipToHeader(
                sipClientProperties.user,
                sipClientProperties.serverIp,
                hostParamsMap = linkedMapOf(
                    "transport" to "UDP"
                ),
            ),
            fromHeader = SipFromHeader(
                user = sipClientProperties.user,
                host = sipClientProperties.serverIp,
                hostParamsMap = linkedMapOf(
                    "transport" to "UDP"
                ),
                fromParamsMap = linkedMapOf(
                    "tag" to fromTag
                )
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
            expires = 60,
            allow = arrayListOf(
                SipMethod.INVITE, SipMethod.ACK, SipMethod.CANCEL, SipMethod.BYE, SipMethod.NOTIFY, SipMethod.REFER,
                SipMethod.MESSAGE, SipMethod.OPTIONS, SipMethod.INFO, SipMethod.SUBSCRIBE)
        )
        send(unregisterRequest.buildString())
        var sipRegisterResponse = registerResponseChannel.receive()
        if (sipRegisterResponse.status == SipStatus.Unauthorized) {
            registerBranch = UUID.randomUUID().toString()
            val newUnregisterRequest = SipRegisterRequest(
                requestURIHeader = SipRequestURIHeader(
                    method = SipMethod.REGISTER,
                    host = sipClientProperties.serverIp,
                    hostParamsMap = linkedMapOf(
                        "transport" to "UDP"
                    )
                ),
                viaHeader = SipViaHeader(
                    host = sipClientProperties.clientIp,
                    port = sipClientProperties.clientPort,
                    hostParams = linkedMapOf(
                        "branch" to registerBranch,
                        "rport" to ""
                    )
                ),
                contactHeader = SipContactHeader(
                    user = sipClientProperties.user,
                    host = sipClientProperties.clientIp,
                    port = sipClientProperties.clientPort,
                    hostParamsMap = linkedMapOf(
                        "rinstance" to rinstance
                    ),
                    contactParamsMap = linkedMapOf(
                        "expires" to "0"
                    )
                ),
                toHeader = SipToHeader(
                    sipClientProperties.user,
                    sipClientProperties.serverIp,
                    hostParamsMap = linkedMapOf(
                        "transport" to "UDP"
                    )
                ),
                fromHeader = SipFromHeader(
                    user = sipClientProperties.user,
                    host = sipClientProperties.serverIp,
                    hostParamsMap = linkedMapOf(
                        "transport" to "UDP"
                    ),
                    fromParamsMap = linkedMapOf(
                        "tag" to fromTag
                    )
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
                expires = 60,
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
        val fromTag = randomString(8)
        val inviteRequest = SipInviteRequest(
            requestURIHeader = SipRequestURIHeader(
                method = SipMethod.INVITE,
                user = remoteUser,
                host = sipClientProperties.serverIp,
                hostParamsMap = linkedMapOf(
                    "transport" to "UDP"
                )
            ),
            viaHeader = SipViaHeader(
                host = sipClientProperties.clientIp,
                port = sipClientProperties.clientPort,
                hostParams = linkedMapOf(
                    "branch" to inviteBranch
                )
            ),
            contactHeader = SipContactHeader(
                user = sipClientProperties.user,
                host = sipClientProperties.clientIp,
                port = sipClientProperties.clientPort
            ),
            toHeader = SipToHeader(
                user = remoteUser,
                host = sipClientProperties.serverIp
            ),
            fromHeader = SipFromHeader(
                user = sipClientProperties.user,
                host = sipClientProperties.serverIp,
                fromParamsMap = linkedMapOf(
                    "tag" to fromTag
                )
            ),
            cSeqHeader = CSeqHeader(
                cSeqNumber = 1,
                method = SipMethod.INVITE
            ),
            maxForwards = 70,
            callIdHeader = CallIdHeader(
                callId = callId
            ),
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
                requestURIHeader = SipRequestURIHeader(
                    method = SipMethod.INVITE,
                    user = remoteUser,
                    host = sipClientProperties.serverIp,
                    hostParamsMap = linkedMapOf(
                        "transport" to "UDP"
                    )
                ),
                viaHeader = SipViaHeader(
                    host = sipClientProperties.clientIp,
                    port = sipClientProperties.clientPort,
                    hostParams = linkedMapOf(
                        "branch" to inviteBranch
                    )
                ),
                contactHeader = SipContactHeader(
                    user = sipClientProperties.user,
                    host = sipClientProperties.clientIp,
                    port = sipClientProperties.clientPort
                ),
                toHeader = SipToHeader(
                    user = remoteUser,
                    host = sipClientProperties.serverIp
                ),
                fromHeader = SipFromHeader(
                    user = sipClientProperties.user,
                    host = sipClientProperties.serverIp,
                    fromParamsMap = linkedMapOf(
                        "tag" to fromTag
                    )
                ),
                maxForwards = 70,
                callIdHeader = CallIdHeader(
                    callId = callId
                ),
                cSeqHeader = CSeqHeader(
                    cSeqNumber = 2,
                    method = SipMethod.INVITE
                ),
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
        val fromTag = randomString(8)
        val ackRequest = SipAckRequest(
            requestURIHeader = SipRequestURIHeader(
                method = SipMethod.ACK,
                user = remoteUser,
                host = sipClientProperties.serverIp,
                hostParamsMap = linkedMapOf(
                    "transport" to "UDP"
                )
            ),
            viaHeader = SipViaHeader(
                host = sipClientProperties.clientIp,
                port = sipClientProperties.clientPort,
                hostParams = linkedMapOf(
                    "branch" to branch,
                    "rport" to ""
                )
            ),
            maxForwards = 70,
            toHeader = SipToHeader(
                user = sipClientProperties.user,
                host = sipClientProperties.serverIp,
                toParamsMap = linkedMapOf(
                    "tag" to branch
                )),
            fromHeader = SipFromHeader(
                user = sipClientProperties.user,
                host = sipClientProperties.serverIp,
                hostParamsMap = linkedMapOf(
                    "transport" to "UDP"
                ),
                fromParamsMap = linkedMapOf(
                    "tag" to fromTag
                )),
            callIdHeader = CallIdHeader(
                callId = callId
            ),
            cSeqHeader = CSeqHeader(
                cSeqNumber = 1,
                method = SipMethod.ACK
            )
        )
        send(ackRequest.buildString())
    }

}