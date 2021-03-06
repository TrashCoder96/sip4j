package ru.stech.obj.ro.ack

import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipRequest
import ru.stech.obj.ro.SipRequestURIHeader
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader

class SipAckRequest(
    requestURIHeader: SipRequestURIHeader,
    viaHeader: SipViaHeader,
    toHeader: SipToHeader,
    fromHeader: SipFromHeader,
    cSeqHeader: CSeqHeader,
    callIdHeader: CallIdHeader,
    maxForwards: Int,
): SipRequest(requestURIHeader, viaHeader, toHeader, fromHeader, cSeqHeader, callIdHeader, maxForwards) {

    override fun buildString(): String {
        return "${requestURIHeader.buildString()}\n" +
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
