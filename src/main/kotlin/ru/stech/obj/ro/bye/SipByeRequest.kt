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
import ru.stech.obj.ro.findFromHeaderLine
import ru.stech.obj.ro.findRequestURIHeader
import ru.stech.obj.ro.findToHeaderLine
import ru.stech.obj.ro.findViaHeaderLine
import ru.stech.obj.ro.parseToCSeqHeader
import ru.stech.obj.ro.parseToCallIdHeader
import ru.stech.obj.ro.parseToFromHeader
import ru.stech.obj.ro.parseToSipRequestURIHeader
import ru.stech.obj.ro.parseToToHeader
import ru.stech.obj.ro.parseToViaHeader
import ru.stech.obj.ro.userAgent
import ru.stech.util.findMaxForwards

class SipByeRequest(
    requestURIHeader: SipRequestURIHeader,
    viaHeader: SipViaHeader,
    toHeader: SipToHeader,
    fromHeader: SipFromHeader,
    private val contactHeader: SipContactHeader? = null,
    cSeqHeader: CSeqHeader,
    callIdHeader: CallIdHeader,
    maxForwards: Int
): SipRequest(requestURIHeader, viaHeader, toHeader, fromHeader, cSeqHeader, callIdHeader, maxForwards) {
    private val contactHeaderLine = contactHeader?.buildString() ?: ""
    override fun buildString(): String {
        return "${requestURIHeader.buildString()}\r\n" +
                "${viaHeader.buildString()}\r\n" +
                "${fromHeader.buildString()}\r\n" +
                "${toHeader.buildString()}\r\n" +
                "$contactHeaderLine\r\n" +
                "${contactHeader?.buildString()}\r\n" +
                "${callIdHeader.buildString()}\r\n" +
                "${cSeqHeader.buildString()}\r\n" +
                "Max-Forwards: $maxForwards\r\n" +
                "User-Agent: ${userAgent}\r\n" +
                "Content-Length: 0\r\n" +
                "\r\n"
    }
}

fun String.parseToByeRequest(): SipByeRequest {
    val requestURIHeaderLine = this.findRequestURIHeader() ?: throw SipParseException()
    val viaHeaderLine = this.findViaHeaderLine()
    val toHeaderLine = this.findToHeaderLine()
    val fromHeaderLine = this.findFromHeaderLine()
    //val contactHeaderLine = this.findContactHeaderLine() ?: throw SipParseException()
    val cSeqHeaderLine = this.findCSeqHeader()
    val callIdHeaderLine = this.findCallIdHeader()
    val maxForwards = findMaxForwards(this)
    return SipByeRequest(
        requestURIHeader = requestURIHeaderLine.parseToSipRequestURIHeader(),
        viaHeader = viaHeaderLine.parseToViaHeader(),
        toHeader = toHeaderLine.parseToToHeader(),
        fromHeader = fromHeaderLine.parseToFromHeader(),
        cSeqHeader = cSeqHeaderLine.parseToCSeqHeader(),
        callIdHeader = callIdHeaderLine.parseToCallIdHeader(),
        maxForwards = maxForwards
    )
}