package ru.stech.obj.ro.register

import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.SipRequest
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader

class SipRegisterRequest(
    viaHeader: SipViaHeader,
    toHeader: SipToHeader,
    fromHeader: SipFromHeader,
    contactHeader: SipContactHeader,
    cSeqHeader: CSeqHeader,
    callIdHeader: CallIdHeader,
    maxForwards: Int,
    val allow: Collection<SipMethod>,
    val expires: Int,
    val authorizationHeader: SipAuthorizationHeader? = null,
): SipRequest(viaHeader, toHeader, fromHeader, contactHeader, cSeqHeader, callIdHeader, maxForwards) {
    override fun buildString(): String {
        return "REGISTER sip:${toHeader.host};transport=UDP SIP/2.0\n" +
                "${viaHeader.buildString()}\n" +
                "Max-Forwards: ${maxForwards}\n" +
                "${contactHeader.buildString()}\n" +
                "${toHeader.buildString()}\n" +
                "${fromHeader.buildString()}\n" +
                "${callIdHeader.buildString()}\n" +
                "${cSeqHeader.buildString()}\n" +
                "Expires: ${expires}\n" +
                "Allow: ${allow.joinToString(", ")}\n" +
                "User-Agent: Sip4j Library\n" +
                (authorizationHeader?.buildString() ?: "") +
                "Allow-Events: presence, kpml, talk\n" +
                "Content-Length: 0\n" +
                "\n"
    }
}