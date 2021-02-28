package ru.stech.obj.ro.ack

import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipToHeader

class SipAckRequest(
    val branch: String,
    val contactHeader: SipContactHeader,
    val toHeader: SipToHeader,
    val fromHeader: SipFromHeader,
    val maxForwards: Int,
    val callId: String,
    val cseqNumber: Int,
) {

    fun buildString(): String {
        return "ACK sip:${toHeader.user}@192.168.188.144:51233 SIP/2.0\n" +
                "Via: SIP/2.0/UDP 10.255.250.29:5060;rport;branch=z9hG4bKPj1b58a169-6c17-49fa-879d-0f74b4469a0d\n" +
                "From: \"${fromHeader.user}\" <sip:${fromHeader.user}@${fromHeader.host}>;tag=a224616c-0cf6-4b5e-8385-1e3a0336615b\n" +
                "To: <sip:4090@192.168.188.144;rinstance=f2f05019bc579703>;tag=454a9b0b\n" +
                "Call-ID: ${callId}\n" +
                "CSeq: $cseqNumber ACK\n" +
                "Max-Forwards: ${maxForwards}\n" +
                "User-Agent: Sip4j Library\n" +
                "Content-Length:  0"
    }
}
