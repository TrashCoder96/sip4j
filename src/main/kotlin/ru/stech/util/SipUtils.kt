package ru.stech.util

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

fun sendSipRequest(requestBody: String,
                   serverHost: String,
                   serverPort: Int,
                   channel: DatagramChannel) {
    val byteBody = requestBody.toByteArray()
    val buf = ByteBuffer.allocate(byteBody.size)
    buf.clear()
    buf.put(byteBody)
    buf.flip()
    channel.send(buf, InetSocketAddress(serverHost, serverPort))
}