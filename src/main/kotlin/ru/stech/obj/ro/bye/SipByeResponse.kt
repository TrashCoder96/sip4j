package ru.stech.obj.ro.bye

import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipResponse
import ru.stech.obj.ro.SipStatus
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader

class SipByeResponse(
    status: SipStatus,
    viaHeader: SipViaHeader,
    fromHeader: SipFromHeader,
    toHeader: SipToHeader,
    val contactHeader: SipContactHeader,
    cSeqHeader: CSeqHeader,
    callIdHeader: CallIdHeader
): SipResponse(status, viaHeader, fromHeader, toHeader, cSeqHeader, callIdHeader) {

    override fun buildString(): String {
        return "SIP/2.0 ${status.status} ${status.name}\n" +
                "${viaHeader.buildString()}\n" +
                "${contactHeader.buildString()}\n" +
                "${fromHeader.buildString()}\n" +
                "${toHeader.buildString()}\n" +
                "${callIdHeader.buildString()}\n" +
                "${cSeqHeader.buildString()}\n" +
                "User-Agent: Sip4j Library\n" +
                "Content-Length: 0\n" +
                "\n"
    }
}