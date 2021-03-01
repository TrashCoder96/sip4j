package ru.stech

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.invite.SipInviteRequest
import ru.stech.obj.ro.options.SipOptionsResponse
import ru.stech.obj.ro.options.branchRegexp
import ru.stech.obj.ro.register.SipRegisterRequest
import ru.stech.obj.ro.register.buildString
import ru.stech.util.findIp
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
    private val channel = DatagramChannel.open()
    private var currentBranchId: String? = null
    private var currentCallId: String? = null
    private val clientIp = findIp()
    private val processingClientTransactions = mutableMapOf<String, Transaction>()
    private val processingServerTransaction = mutableMapOf<String, ServerTransaction>()

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
    }

    fun register() {
        currentCallId = UUID.randomUUID().toString()
        val registerTransaction = RegisterTransaction(currentCallId!!, channel, sipClientProperties)
        processingClientTransactions[registerTransaction.branch] = registerTransaction
        registerTransaction.start()
    }

    fun call() {
        val inviteTransaction = InviteTransaction(remoteUser, currentCallId!!, channel, sipClientProperties)
        processingClientTransactions[inviteTransaction.branch] = inviteTransaction
        inviteTransaction.start()
    }

    fun startListening() {
        val receivedBuf = ByteBuffer.allocate(2048)
        CoroutineScope(dispatcher).launch {
            while (true) {
                receivedBuf.clear()
                if (channel.receive(receivedBuf) != null) {
                    val body = String(receivedBuf.array())
                    val branch = extractBranchFromReceivedBody(body)
                    if (processingClientTransactions[branch] != null) {
                        //this is response from proxy server, continue operation
                        processingClientTransactions[branch]?.processReceivedBody(branch, body)
                    } else {
                        val callId = extractCallIdFromReceivedBody(body)
                        processRequest(branch, callId, body)
                    }
                }
            }
        }
        print("started")
    }

    private fun processRequest(branch: String, callId: String, body: String) {
        if (body.startsWith(SipMethod.OPTIONS.name)) {
            val optionsTransaction = OptionsServerTransaction(callId, channel, sipClientProperties)
            processingServerTransaction[branch] = optionsTransaction
            optionsTransaction.askRequest(branch, body)
        } else {
            throw IllegalArgumentException()
        }
    }

    private fun extractBranchFromReceivedBody(body: String): String {
        val result = branchRegexp.find(body)
        return result!!.groupValues[1]
    }

    private fun extractCallIdFromReceivedBody(body: String): String {
        val result = branchRegexp.find(body)
        return result!!.groupValues[1]
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

    private fun sendInviteRequest(request: SipInviteRequest) {
        val buf = ByteBuffer.allocate(2048)
        buf.clear()
        buf.put(request.buildString().toByteArray())
        buf.flip()
        channel.send(buf, InetSocketAddress(serverIp, serverPort))
    }


}