package ru.stech.obj.ro.options

import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipRequest
import ru.stech.obj.ro.SipRequestURIHeader
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader
import ru.stech.obj.ro.findCSeqHeader
import ru.stech.obj.ro.findCallIdHeader
import ru.stech.obj.ro.findContactHeaderLine
import ru.stech.obj.ro.findFromHeaderLine
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

class SipOptionsRequest(
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
        TODO("Not yet implemented")
    }
}

fun String.parseToOptionsRequest(): SipOptionsRequest {
    val requestURIHeader = this.parseToSipRequestURIHeader()
    val viaHeaderLine = this.findViaHeaderLine()
    val fromHeaderLine = this.findFromHeaderLine()
    val toHeaderLine = this.findToHeaderLine()
    val callIdHeaderLine = this.findCallIdHeader()
    val cSeqNumberHeaderLine = this.findCSeqHeader()
    val maxForwards = findMaxForwards(this)
    val contactHeaderLine = this.findContactHeaderLine()
    return SipOptionsRequest(
        requestURIHeader = requestURIHeader,
        viaHeader = viaHeaderLine.parseToViaHeader(),
        fromHeader = fromHeaderLine.parseToFromHeader(),
        contactHeader = contactHeaderLine.parseToContactHeader(),
        toHeader = toHeaderLine.parseToToHeader(),
        callIdHeader = callIdHeaderLine.parseToCallIdHeader(),
        cSeqHeader = cSeqNumberHeaderLine.parseToCSeqHeader(),
        maxForwards = maxForwards
    )
}