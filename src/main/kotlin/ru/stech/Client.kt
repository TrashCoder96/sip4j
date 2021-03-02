package ru.stech

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.options.callIdRegexp
import ru.stech.util.findIp
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

    suspend fun startRegister() {
        val registerOperation = RegisterOperation(channel, sipClientProperties)
        processingClientOperations[registerOperation.callId] = registerOperation
        registerOperation.start()
        return suspendCoroutine {
            while (!registerOperation.isCompleted()) {}
            it.resume(Unit)
        }
    }

    suspend fun unregister() {

    }

    suspend fun call() {
        val inviteOperation = InviteOperation(remoteUser, channel, sipClientProperties)
        processingClientOperations[inviteOperation.callId] = inviteOperation
        inviteOperation.start()
        return suspendCoroutine {
            while (!inviteOperation.isCompleted()) {}
            it.resume(Unit)
        }
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
                        processingClientOperations[callId]!!.processReceivedBody(body)
                        //remove operation if completed
                        if (processingClientOperations[callId]!!.isCompleted()) {
                            processingClientOperations.remove(callId)
                        }
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
            processingServerOperations[callId] = optionsTransaction
            optionsTransaction.askRequest(callId, body)
        } else {
            throw IllegalArgumentException()
        }
    }

    private fun extractCallIdFromReceivedBody(body: String): String {
        val result = callIdRegexp.find(body)
        return result!!.groupValues[1]
    }


}