package ru.stech.obj.ro.options

import ru.stech.obj.ro.SipStatus
import java.util.*

class SipOptionsResponse(
    val user: String,
    val status: SipStatus,
    val serverIp: String,
    val serverPort: Int,
    val clientIp: String,
    val clientPort: Int,
    val branch: String,
    val fromTag: String,
    val toTag: String = UUID.randomUUID().toString(),
    val callId: String,
    val cseqNumber: Int
) {
    fun buildString(): String {
        return "SIP/2.0 ${status.status} ${status.name}\n" +
                "Via: SIP/2.0/UDP ${serverIp}:${serverPort};branch=${branch}\n" +
                "Contact: <sip:${clientIp}:${clientPort}>\n" +
                "To: <sip:${user}@${clientIp}>;tag=${toTag}\n" +
                "From: <sip:${user}@${serverIp}>;tag=${fromTag}\n" +
                "Call-ID: ${callId}\n" +
                "CSeq: $cseqNumber OPTIONS\n" +
                "Accept: application/sdp, application/sdp\n" +
                "Accept-Language: en\n" +
                "Allow: INVITE, ACK, CANCEL, BYE, NOTIFY, REFER, MESSAGE, OPTIONS, INFO, SUBSCRIBE\n" +
                "Supported: replaces, norefersub, extended-refer, timer, outbound, path, X-cisco-serviceuri\n" +
                "User-Agent: Sip4j Library\n" +
                "Allow-Events: presence, kpml, talk\n" +
                "Content-Length: 0\n" +
                "\n"
    }
}
