package ru.stech.sip

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.epoll.EpollDatagramChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import ru.stech.SipClientProperties
import ru.stech.g711.DecompressInputStream
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
import ru.stech.util.sendSipRequest
import java.io.ByteArrayInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.*

class Client (
    private val user: String,
    private val password: String,
    private val clientPort: Int,
    private val serverIp: String,
    private val serverPort: Int,
    private val dispatcher: CoroutineDispatcher
) {
    private val sipChannel = DatagramChannel.open()
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

    val sipClientProperties: SipClientProperties = SipClientProperties(
        user = user,
        password = password,
        serverIp = serverIp,
        serverPort = serverPort,
        clientIp = clientIp,
        clientPort = clientPort,
        rtpPort = 30040
    )
    private val remoteUser = "4090"
    private val sessionTimeout = 60

    private val f = FileOutputStream("file.wav")

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
                        sendSipRequest(optionsResponse.buildString(),
                            sipClientProperties.serverIp, sipClientProperties.serverPort, channel)
                    }
                    byeRequestChannel.onReceive {
                        f.close()
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
                        sendSipRequest(byeResponse.buildString())
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
                    val data = Arrays.copyOfRange(receivedBuf.array(), 12, receivedBuf.position())
                    val stream = ByteArrayInputStream(data)
                    val inp = DecompressInputStream(stream, true)
                    f.write(inp.readAllBytes())
                }
            }
        }
    }

    private var sipChannelFuture: ChannelFuture? = null

    private fun startListening() {
        val sipWorkerGroup = EpollEventLoopGroup()
        val bootstrap = ServerBootstrap().group(sipWorkerGroup)
            .channel(EpollDatagramChannel::class.java)
            .handler(null)
        sipChannelFuture = bootstrap.bind(sipClientProperties.clientPort)
            .syncUninterruptibly()
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
        startListening()
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
            expires = sessionTimeout,
            allow = arrayListOf(
                SipMethod.INVITE, SipMethod.ACK, SipMethod.CANCEL, SipMethod.BYE, SipMethod.NOTIFY, SipMethod.REFER,
                SipMethod.MESSAGE, SipMethod.OPTIONS, SipMethod.INFO, SipMethod.SUBSCRIBE)
        )
        sendSipRequest(request.buildString())
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
                expires = sessionTimeout,
                allow = arrayListOf(
                    SipMethod.INVITE, SipMethod.ACK, SipMethod.CANCEL, SipMethod.BYE, SipMethod.NOTIFY, SipMethod.REFER,
                    SipMethod.MESSAGE, SipMethod.OPTIONS, SipMethod.INFO, SipMethod.SUBSCRIBE)
            )
            sendSipRequest(newRegisterRequest.buildString())
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
            expires = sessionTimeout,
            allow = arrayListOf(
                SipMethod.INVITE, SipMethod.ACK, SipMethod.CANCEL, SipMethod.BYE, SipMethod.NOTIFY, SipMethod.REFER,
                SipMethod.MESSAGE, SipMethod.OPTIONS, SipMethod.INFO, SipMethod.SUBSCRIBE)
        )
        sendSipRequest(unregisterRequest.buildString())
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
            sendSipRequest(newUnregisterRequest.buildString())
            sipRegisterResponse = registerResponseChannel.receive()
        }
        if (sipRegisterResponse.status == SipStatus.OK) {
            print("Unregistration is ok")
        } else {
            print("Unregistration is failed")
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
        sendSipRequest(ackRequest.buildString())
    }

}