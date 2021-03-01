package ru.stech

import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.SipStatus
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.options.SipOptionsResponse
import ru.stech.obj.ro.options.parseToOptionsRequest
import ru.stech.obj.ro.register.SipAuthorizationHeader
import ru.stech.obj.ro.register.SipRegisterRequest
import ru.stech.obj.ro.register.SipRegisterResponse
import ru.stech.obj.ro.register.buildString
import ru.stech.obj.ro.register.parseToSipRegisterResponse
import ru.stech.util.findIp
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.security.MessageDigest
import java.util.UUID

open class Client (
    private val user: String,
    private val password: String,
    private val clientPort: Int,
    private val serverIp: String,
    private val serverPort: Int,
) {
    private val channel = DatagramChannel.open()
    private var currentCallId: String? = null
    private var currentBranchId: String? = null
    private val clientIp = findIp()
    private var processingClientTransactions = mutableMapOf<SipMethod, SipTransaction>()
    private var isRegistered = false

    init {
        channel.socket().bind(InetSocketAddress(clientPort))
    }

    fun initRegister() {
        val registerTransaction = SipTransaction()
        processingClientTransactions[SipMethod.REGISTER] = registerTransaction

        currentCallId = UUID.randomUUID().toString()
        currentBranchId = UUID.randomUUID().toString()
        val request = SipRegisterRequest(
            method = SipMethod.REGISTER,
            serverIp= serverIp,
            clientIp = clientIp,
            clientPort = clientPort,
            branchIdPart = currentBranchId!!,
            maxForwards = 70,
            contactHeader = SipContactHeader(user, clientIp, clientPort),
            toHeader = SipToHeader(user, serverIp),
            fromHeader = SipFromHeader(user, serverIp),
            callId = registerTransaction.callId,
            cSeqOrder = registerTransaction.nextNumber(),
            expires = 60,
            allow = arrayListOf(
                SipMethod.INVITE, SipMethod.ACK, SipMethod.CANCEL, SipMethod.BYE, SipMethod.NOTIFY, SipMethod.REFER,
                SipMethod.MESSAGE, SipMethod.OPTIONS, SipMethod.INFO, SipMethod.SUBSCRIBE)
        )
        sendRegisterRequest(request)
    }

    fun startListening() {
        while (true) {
            val receivedBuf = ByteBuffer.allocate(1024)
            receivedBuf.clear()
            if (channel.receive(receivedBuf) != null) {
                val str = String(receivedBuf.array())
                if (isRequest(str)) {
                    val optionsRequest = str.parseToOptionsRequest()
                    val optionsResponse = SipOptionsResponse(
                        user = user,
                        status = SipStatus.OK,
                        serverIp = serverIp,
                        serverPort = serverPort,
                        clientIp = clientIp,
                        clientPort = clientPort,
                        branch = optionsRequest.branch,
                        fromTag = optionsRequest.tag,
                        callId = optionsRequest.callId,
                        cseqNumber = optionsRequest.cseqNumber
                    )
                    sendOptionsResponse(optionsResponse)
                } else {
                    val registerResponse = str.parseToSipRegisterResponse()
                    when (registerResponse.status) {
                        SipStatus.OK -> processOk(registerResponse)
                        SipStatus.Unauthorized -> processUnauthorized(registerResponse)
                        else -> throw IllegalArgumentException("Error!!!")
                    }
                }
            }
        }
    }

    private fun isRequest(body: String): Boolean {
        for (method in SipMethod.values()) {
            if (body.startsWith(method.name)) {
                return true
            }
        }
        return false
    }

    private fun sendOptionsResponse(response: SipOptionsResponse) {
        val buf = ByteBuffer.allocate(1024)
        buf.clear()
        buf.put(response.buildString().toByteArray())
        buf.flip()
        channel.send(buf, InetSocketAddress(serverIp, serverPort))
    }

    private fun sendRegisterRequest(request: SipRegisterRequest) {
        val buf = ByteBuffer.allocate(1024)
        buf.clear()
        buf.put(request.buildString().toByteArray())
        buf.flip()
        channel.send(buf, InetSocketAddress(serverIp, serverPort))
    }

    private fun processOk(response: SipRegisterResponse) {
        print("Registration is ok!!!")
        isRegistered = true
    }

    private fun processUnauthorized(response: SipRegisterResponse) {
        val cnonce = UUID.randomUUID().toString()
        val ha1 = md5("${user}:${response.wwwAuthenticateHeader!!.realm}:${password}")
        val ha2 = md5("${SipMethod.REGISTER.name}:sip:${serverIp};transport=UDP")
        val responseHash = md5("${ha1}:${response.wwwAuthenticateHeader.nonce}:00000001:${cnonce}:${response.wwwAuthenticateHeader.qop}:${ha2}")
        val request = SipRegisterRequest(
            method = SipMethod.REGISTER,
            serverIp= serverIp,
            clientIp = clientIp,
            clientPort = clientPort,
            branchIdPart = currentBranchId!!,
            maxForwards = 70,
            contactHeader = SipContactHeader(user, clientIp, clientPort),
            toHeader = SipToHeader(user, serverIp),
            fromHeader = SipFromHeader(user, serverIp),
            callId = currentCallId!!,
            cSeqOrder = processingClientTransactions[SipMethod.REGISTER]!!.nextNumber(),
            expires = 60,
            allow = arrayListOf(
                SipMethod.INVITE, SipMethod.ACK, SipMethod.CANCEL, SipMethod.BYE, SipMethod.NOTIFY, SipMethod.REFER,
                SipMethod.MESSAGE, SipMethod.OPTIONS, SipMethod.INFO, SipMethod.SUBSCRIBE),
            authorizationHeader = SipAuthorizationHeader(user = user,
                realm = response.wwwAuthenticateHeader.realm,
                nonce = response.wwwAuthenticateHeader.nonce,
                serverIp = serverIp,
                response = responseHash,
                cnonce = cnonce,
                nc = "00000001",
                qop = response.wwwAuthenticateHeader.qop,
                algorithm = response.wwwAuthenticateHeader.algorithm,
                opaque = response.wwwAuthenticateHeader.opaque
            )
        )
        sendRegisterRequest(request)
    }

    fun md5(input:String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

}