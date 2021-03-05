package ru.stech.obj.ro.register

import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.SipRequest
import ru.stech.obj.ro.SipRequestURIHeader
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader

class SipRegisterRequest(
    requestURIHeader: SipRequestURIHeader,
    viaHeader: SipViaHeader,
    toHeader: SipToHeader,
    fromHeader: SipFromHeader,
    val contactHeader: SipContactHeader,
    cSeqHeader: CSeqHeader,
    callIdHeader: CallIdHeader,
    maxForwards: Int,
    val allow: Collection<SipMethod>,
    val expires: Int,
    val authorizationHeader: SipAuthorizationHeader? = null,
): SipRequest(requestURIHeader, viaHeader, toHeader, fromHeader, cSeqHeader, callIdHeader, maxForwards) {
    override fun buildString(): String {
        return "${requestURIHeader.buildString()}\n" +
                "${viaHeader.buildString()}\n" +
                "${contactHeader.buildString()}\n" +
                "${toHeader.buildString()}\n" +
                "${fromHeader.buildString()}\n" +
                "${callIdHeader.buildString()}\n" +
                "${cSeqHeader.buildString()}\n" +
                "Max-Forwards: ${maxForwards}\n" +
                "Expires: ${expires}\n" +
                "Allow: ${allow.joinToString(", ")}\n" +
                "User-Agent: Sip4j Library\n" +
                (authorizationHeader?.buildString() ?: "") +
                "Allow-Events: presence, kpml, talk\n" +
                "Content-Length: 0\n" +
                "\n"
    }
}