package ru.stech.obj.ro.bye

import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipRequest
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader
import ru.stech.util.extractBranchFromReceivedBody
import ru.stech.util.extractCSeqNumberFromReceivedBody
import ru.stech.util.extractCallIdFromReceived
import ru.stech.util.extractFromHeaderfromReceivedBody

class SipByeRequest(
    viaHeader: SipViaHeader,
    toHeader: SipToHeader,
    fromHeader: SipFromHeader,
    contactHeader: SipContactHeader,
    cSeqHeader: CSeqHeader,
    callIdHeader: CallIdHeader,
    maxForwards: Int
): SipRequest(viaHeader, toHeader, fromHeader, contactHeader, cSeqHeader, callIdHeader, maxForwards) {
    override fun buildString(): String {
        return "BYE sip:4091@192.168.188.163:63388 SIP/2.0\n" +
                "${viaHeader.buildString()}\n" +
                "${fromHeader.buildString()}\n" +
                "${toHeader.buildString()}\n" +
                "${callIdHeader.buildString()}\n" +
                "${cSeqHeader.buildString()}\n" +
                "Max-Forwards: ${maxForwards}n" +
                "User-Agent: Sip4j Library\n" +
                "Content-Length: 0\n" +
                "\n"
    }
}

fun String.parseToByeRequest(): SipByeRequest {
    val branch = extractBranchFromReceivedBody(this)
    val callId = extractCallIdFromReceived(this)
    val cseqNumber = extractCSeqNumberFromReceivedBody(this)
    val fromHeaderLine = extractFromHeaderfromReceivedBody(this)
    return SipByeRequest(
        branch = branch,
        fromHeader = fromHeaderLine.parseFromHeader(),
        callId = callId,
        cseqNumber = cseqNumber
    )
}