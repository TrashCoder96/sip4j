package ru.stech

import java.nio.channels.DatagramChannel

class UnregisterOperation (
    val datagramChannel: DatagramChannel,
    val sipClientProperties: SipClientProperties
): Operation {
    override fun isCompleted(): Boolean {
        TODO("Not yet implemented")
    }

    override fun start() {
        TODO("Not yet implemented")
    }

    override fun processReceivedBody(body: String) {
        TODO("Not yet implemented")
    }

}