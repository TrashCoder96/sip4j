package ru.stech

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.newFixedThreadPoolContext
import ru.stech.sip.Client

class ClientFactory(nThreads: Int) {
    private val coroutineDispatcher = newFixedThreadPoolContext(nThreads, "Sip4k-coroutine-dispatcher")

    fun newClient(user: String,
                  password: String,
                  clientPort: Int,
                  serverIp: String,
                  serverPort: Int): Client {
        return Client(
            user = user,
            password = password,
            clientPort = clientPort,
            serverPort = serverPort,
            serverIp = serverIp,
            dispatcher = coroutineDispatcher
        )
    }

    fun newClient(user: String,
                  password: String,
                  clientPort: Int,
                  serverIp: String,
                  serverPort: Int,
                  coroutineDispatcher: CoroutineDispatcher
    ): Client {
        return Client(
            user = user,
            password = password,
            clientPort = clientPort,
            serverPort = serverPort,
            serverIp = serverIp,
            dispatcher = coroutineDispatcher
        )
    }

}