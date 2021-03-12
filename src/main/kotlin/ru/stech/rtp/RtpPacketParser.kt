package ru.stech.rtp

import ru.stech.g711.compressor.CompressInputStream
import java.io.ByteArrayInputStream

class RtpPacketParser {
    fun parseBytes(bytes: ByteArray, headers: RtpPacketHeaders, useALaw: Boolean): Array<RtpPacket> {
        val cis = CompressInputStream(ByteArrayInputStream(bytes), useALaw)
        val result = mutableListOf<RtpPacket>()
        cis.use { compressInputStream: CompressInputStream ->
            val payload = compressInputStream.readAllBytes()
            val packet = RtpPacket(headers)
            packet.CSRCCount = (Math.ceil((payload.size / 4).toDouble()).toInt().toByte())
            packet.payload = payload
            result.add(packet)
        }
        return result.toTypedArray()
    }
}