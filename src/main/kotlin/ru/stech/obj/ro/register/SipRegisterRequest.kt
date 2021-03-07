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
        return "${requestURIHeader.buildString()}\r\n" +
                "${viaHeader.buildString()}\r\n" +
                "Max-Forwards: ${maxForwards}\r\n" +
                "${contactHeader.buildString()}\r\n" +
                "${toHeader.buildString()}\r\n" +
                "${fromHeader.buildString()}\r\n" +
                "${callIdHeader.buildString()}\r\n" +
                "${cSeqHeader.buildString()}\r\n" +
                "Expires: ${expires}\r\n" +
                "Allow: ${allow.joinToString(", ")}\r\n" +
                "User-Agent: Z 5.4.12 v2.10.13.2-mod\r\n" +
                (authorizationHeader?.buildString() ?: "") +
                "Allow-Events: presence, kpml, talk\r\n" +
                "Content-Length: 0\r\n" +
                "\r\n"
    }
}