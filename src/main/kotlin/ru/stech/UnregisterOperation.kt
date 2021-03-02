package ru.stech

import java.nio.channels.DatagramChannel

class UnregisterOperation(
    val datagramChannel: DatagramChannel,
    val sipClientProperties: SipClientProperties
): Operation {

}