package ru.stech.obj.ro.options

import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipResponse
import ru.stech.obj.ro.SipStatus
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader

class SipOptionsResponse(
    status: SipStatus,
    viaHeader: SipViaHeader,
    fromHeader: SipFromHeader,
    toHeader: SipToHeader,
    cSeqHeader: CSeqHeader,
    callIdHeader: CallIdHeader,
): SipResponse(status, viaHeader, fromHeader, toHeader, cSeqHeader, callIdHeader) {
    override fun buildString(): String {
        return "SIP/2.0 ${status.status} ${status.name}\n" +
                "${viaHeader.buildString()}\n" +
                "${toHeader.buildString()}\n" +
                "${fromHeader.buildString()}\n" +
                "${callIdHeader.buildString()}\n" +
                "${cSeqHeader.buildString()}\n" +
                "Accept: application/sdp\n" +
                "Accept-Language: en\n" +
                "Allow: INVITE, ACK, CANCEL, BYE, NOTIFY, REFER, MESSAGE, OPTIONS, INFO, SUBSCRIBE\n" +
                "Supported: replaces, norefersub, extended-refer, timer, outbound, path, X-cisco-serviceuri\n" +
                "User-Agent: Sip4j Library\n" +
                "Allow-Events: presence, kpml, talk\n" +
                "Content-Length: 0\n" +
                "\n"
    }
}
