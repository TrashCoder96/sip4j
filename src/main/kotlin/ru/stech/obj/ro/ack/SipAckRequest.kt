package ru.stech.obj.ro.ack

import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.buildString

class SipAckRequest(
    val clientIp: String,
    val clientPort: Int,
    val branch: String,
    val toHeader: SipToHeader,
    val fromHeader: SipFromHeader,
    val maxForwards: Int,
    val callId: String,
    val cseqNumber: Int,
) {

    fun buildString(): String {
        return "ACK sip:${toHeader.user}@${toHeader.host};transport=UDP SIP/2.0\n" +
                "Via: SIP/2.0/UDP ${clientIp}:${clientPort};branch=${branch}\n" +
                "Max-Forwards: ${maxForwards}\n" +
                "${toHeader.buildString()}\n" +
                "${fromHeader.buildString()}\n" +
                "Call-ID: ${callId}\n" +
                "CSeq: $cseqNumber ACK\n" +
                "Content-Length: 0"
    }
}
