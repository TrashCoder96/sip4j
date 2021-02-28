package ru.stech

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import java.util.concurrent.ConcurrentHashMap

class ClientExecutor {
    private val userId = "4091"
    private val password = "E5bTUEKL8K"
    private val clientPort = 30002
    private val serverHost = "10.255.250.29"
    private val serverPort = 5060
    private val dispatcher = newFixedThreadPoolContext(10, "co")
    private val clients: MutableMap<String, Client> = ConcurrentHashMap()

    fun startClient() {
        val client = Client(userId, password, clientPort, serverHost, serverPort)
        clients[userId] = client
        CoroutineScope(dispatcher).launch {
            client.startListening()
        }
        client.initRegister()
    }

    fun start() {
        while (true) {
        }
    }

}