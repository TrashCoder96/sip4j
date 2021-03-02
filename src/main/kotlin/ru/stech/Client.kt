package ru.stech

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.options.branchRegexp
import ru.stech.obj.ro.options.callIdRegexp
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
    private var currentCallId: String? = null
    private val clientIp = findIp()
    private val processingClientOperations = mutableMapOf<String, Operation>()
    private val processingServerOperations = mutableMapOf<String, ServerOperation>()

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
        val registerOperation = RegisterOperation(currentCallId!!, channel, sipClientProperties)
        processingClientOperations[registerOperation.branch] = registerOperation
        registerOperation.start()
    }

    fun call() {
        val inviteTransaction = InviteOperation(remoteUser, currentCallId!!, channel, sipClientProperties)
        processingClientOperations[inviteTransaction.branch] = inviteTransaction
        inviteTransaction.start()
    }

    fun startListening() {
        val receivedBuf = ByteBuffer.allocate(2048)
        CoroutineScope(dispatcher).launch {
            while (true) {
                receivedBuf.clear()
                if (channel.receive(receivedBuf) != null) {
                    val body = String(receivedBuf.array())
                    val callId = extractCallIdFromReceivedBody(body)
                    if (processingClientOperations[callId] != null) {
                        //this is response from proxy server, continue operation
                        processingClientOperations[callId]?.processReceivedBody(body)
                    } else {
                        //this is request from server, create response
                        processRequest(callId, body)
                    }
                }
            }
        }
        print("started")
    }

    private fun processRequest(callId: String, body: String) {
        if (body.startsWith(SipMethod.OPTIONS.name)) {
            val optionsTransaction = OptionsServerOperation(callId, channel, sipClientProperties)
            processingServerOperations[branch] = optionsTransaction
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
        val result = callIdRegexp.find(body)
        return result!!.groupValues[1]
    }


}