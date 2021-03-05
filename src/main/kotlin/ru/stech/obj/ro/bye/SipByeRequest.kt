package ru.stech.obj.ro.bye

import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipParseException
import ru.stech.obj.ro.SipRequest
import ru.stech.obj.ro.SipRequestURIHeader
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader
import ru.stech.obj.ro.findCSeqHeader
import ru.stech.obj.ro.findCallIdHeader
import ru.stech.obj.ro.findContactHeaderLine
import ru.stech.obj.ro.findFromHeaderLine
import ru.stech.obj.ro.findRequestURIHeader
import ru.stech.obj.ro.findToHeaderLine
import ru.stech.obj.ro.findViaHeaderLine
import ru.stech.obj.ro.parseToCSeqHeader
import ru.stech.obj.ro.parseToCallIdHeader
import ru.stech.obj.ro.parseToContactHeader
import ru.stech.obj.ro.parseToFromHeader
import ru.stech.obj.ro.parseToSipRequestURIHeader
import ru.stech.obj.ro.parseToToHeader
import ru.stech.obj.ro.parseToViaHeader
import ru.stech.util.findMaxForwards

class SipByeRequest(
    requestURIHeader: SipRequestURIHeader,
    viaHeader: SipViaHeader,
    toHeader: SipToHeader,
    fromHeader: SipFromHeader,
    val contactHeader: SipContactHeader,
    cSeqHeader: CSeqHeader,
    callIdHeader: CallIdHeader,
    maxForwards: Int
): SipRequest(requestURIHeader, viaHeader, toHeader, fromHeader, cSeqHeader, callIdHeader, maxForwards) {
    override fun buildString(): String {
        return "${requestURIHeader.buildString()}\n" +
                "${viaHeader.buildString()}\n" +
                "${fromHeader.buildString()}\n" +
                "${toHeader.buildString()}\n" +
                "${contactHeader.buildString()}\n" +
                "${callIdHeader.buildString()}\n" +
                "${cSeqHeader.buildString()}\n" +
                "Max-Forwards: ${maxForwards}n" +
                "User-Agent: Sip4j Library\n" +
                "Content-Length: 0\n" +
                "\n"
    }
}

fun String.parseToByeRequest(): SipByeRequest {
    val requestURIHeaderLine = this.findRequestURIHeader() ?: throw SipParseException()
    val viaHeaderLine = this.findViaHeaderLine() ?: throw SipParseException()
    val toHeaderLine = this.findToHeaderLine() ?: throw SipParseException()
    val fromHeaderLine = this.findFromHeaderLine() ?: throw SipParseException()
    val contactHeaderLine = this.findContactHeaderLine() ?: throw SipParseException()
    val cSeqHeaderLine = this.findCSeqHeader() ?: throw SipParseException()
    val callIdHeaderLine = this.findCallIdHeader() ?: throw SipParseException()
    val maxForwards = findMaxForwards(this)
    return SipByeRequest(
        requestURIHeader = requestURIHeaderLine.parseToSipRequestURIHeader(),
        viaHeader = viaHeaderLine.parseToViaHeader(),
        toHeader = toHeaderLine.parseToToHeader(),
        fromHeader = fromHeaderLine.parseToFromHeader(),
        contactHeader = contactHeaderLine.parseToContactHeader(),
        cSeqHeader = cSeqHeaderLine.parseToCSeqHeader(),
        callIdHeader = callIdHeaderLine.parseToCallIdHeader(),
        maxForwards = maxForwards
    )
}