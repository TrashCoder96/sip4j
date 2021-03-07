package ru.stech.obj.ro.options

import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipResponse
import ru.stech.obj.ro.SipStatus
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader

class SipOptionsResponse(
    status: SipStatus,
    viaHeader: SipViaHeader,
    val contactHeader: SipContactHeader,
    fromHeader: SipFromHeader,
    toHeader: SipToHeader,
    cSeqHeader: CSeqHeader,
    callIdHeader: CallIdHeader,
): SipResponse(status, viaHeader, fromHeader, toHeader, cSeqHeader, callIdHeader) {
    override fun buildString(): String {
        return "SIP/2.0 ${status.status} ${status.name}\r\n" +
                "${viaHeader.buildString()}\r\n" +
                "${contactHeader.buildString()}\r\n" +
                "${toHeader.buildString()}\r\n" +
                "${fromHeader.buildString()}\r\n" +
                "${callIdHeader.buildString()}\r\n" +
                "${cSeqHeader.buildString()}\r\n" +
                "Accept: application/sdp, application/sdp\r\n" +
                "Accept-Language: en\r\n" +
                "Allow: INVITE, ACK, CANCEL, BYE, NOTIFY, REFER, MESSAGE, OPTIONS, INFO, SUBSCRIBE\r\n" +
                "Supported: replaces, norefersub, extended-refer, timer, outbound, path, X-cisco-serviceuri\r\n" +
                "User-Agent: Z 5.4.12 v2.10.13.2-mod\r\n" +
                "Allow-Events: presence, kpml, talk\r\n" +
                "Content-Length: 0\r\n" +
                "\r\n"
    }
}
