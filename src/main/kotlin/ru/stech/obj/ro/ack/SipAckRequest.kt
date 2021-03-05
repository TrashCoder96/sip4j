package ru.stech.obj.ro.ack

import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipRequest
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader

class SipAckRequest(
    viaHeader: SipViaHeader,
    toHeader: SipToHeader,
    fromHeader: SipFromHeader,
    contactHeader: SipContactHeader,
    cSeqHeader: CSeqHeader,
    callIdHeader: CallIdHeader,
    maxForwards: Int,
): SipRequest(viaHeader, toHeader, fromHeader, contactHeader, cSeqHeader, callIdHeader, maxForwards) {

    override fun buildString(): String {
        return "ACK sip:${toHeader.user}@${toHeader.host};transport=UDP SIP/2.0\n" +
                "${viaHeader.buildString()}\n" +
                "${toHeader.buildString()}\n" +
                "${fromHeader.buildString()}\n" +
                "Max-Forwards: ${maxForwards}\n" +
                "${callIdHeader.buildString()}\n" +
                "${cSeqHeader.buildString()}\n" +
                "Content-Length: 0\n" +
                "\n"
    }

}
