package ru.stech.obj.ro.register

import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.buildString

data class SipRegisterRequest(
    val branch: String,
    val maxForwards: Int,
    val contactHeader: SipContactHeader,
    val toHeader: SipToHeader,
    val fromHeader: SipFromHeader,
    val callId: String,
    val cSeqOrder: Int,
    val expires: Int,
    val allow: Collection<SipMethod>,
    val authorizationHeader: SipAuthorizationHeader? = null
)

fun SipRegisterRequest.buildString(): String {
    return "REGISTER sip:${toHeader.host};transport=UDP SIP/2.0\n" +
            "Via: SIP/2.0/UDP ${contactHeader.localIp}:${contactHeader.localPort};branch=${branch}\n" +
            "Max-Forwards: ${maxForwards}\n" +
            "${contactHeader.buildString()}\n" +
            "${toHeader.buildString()}\n" +
            "${fromHeader.buildString()}\n" +
            "Call-ID: ${callId}\n" +
            "CSeq: $cSeqOrder REGISTER\n" +
            "Expires: ${expires}\n" +
            "Allow: ${allow.joinToString(", ")}\n" +
            "User-Agent: Sip4j Library\n" +
            (authorizationHeader?.buildString() ?: "") +
            "Allow-Events: presence, kpml, talk\n" +
            "Content-Length: 0\n" +
            "\n"
}