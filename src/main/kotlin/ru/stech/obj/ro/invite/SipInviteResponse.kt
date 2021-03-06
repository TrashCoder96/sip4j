package ru.stech.obj.ro.invite

import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipResponse
import ru.stech.obj.ro.SipStatus
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader
import ru.stech.obj.ro.findCSeqHeader
import ru.stech.obj.ro.findCallIdHeader
import ru.stech.obj.ro.findFromHeaderLine
import ru.stech.obj.ro.findResponseURIHeader
import ru.stech.obj.ro.findToHeaderLine
import ru.stech.obj.ro.findViaHeaderLine
import ru.stech.obj.ro.parseResponseSipStatus
import ru.stech.obj.ro.parseToCSeqHeader
import ru.stech.obj.ro.parseToCallIdHeader
import ru.stech.obj.ro.parseToFromHeader
import ru.stech.obj.ro.parseToToHeader
import ru.stech.obj.ro.parseToViaHeader
import ru.stech.obj.ro.register.WWWAuthenticateHeader
import ru.stech.obj.ro.register.findWWWAuthenticateHeader
import ru.stech.obj.ro.register.parseToWWWAuthenticateHeader

class SipInviteResponse(
    status: SipStatus,
    viaHeader: SipViaHeader,
    fromHeader: SipFromHeader,
    toHeader: SipToHeader,
    cSeqHeader: CSeqHeader,
    callIdHeader: CallIdHeader,
    val wwwAuthenticateHeader: WWWAuthenticateHeader? = null,
): SipResponse(status, viaHeader, fromHeader, toHeader, cSeqHeader, callIdHeader) {
    override fun buildString(): String {
        TODO("Not yet implemented")
    }
}

fun String.parseToInviteResponse(): SipInviteResponse {
    val responseURIHeaderLine = this.findResponseURIHeader()
    val viaHeaderLine = this.findViaHeaderLine()
    val fromHeaderLine = this.findFromHeaderLine()
    val toHeaderLine = this.findToHeaderLine()
    val cSeqHeaderLine = this.findCSeqHeader()
    val callIdHeaderLine = this.findCallIdHeader()
    return SipInviteResponse(
        status = responseURIHeaderLine.parseResponseSipStatus(),
        viaHeader = viaHeaderLine.parseToViaHeader(),
        fromHeader = fromHeaderLine.parseToFromHeader(),
        toHeader = toHeaderLine.parseToToHeader(),
        cSeqHeader = cSeqHeaderLine.parseToCSeqHeader(),
        callIdHeader = callIdHeaderLine.parseToCallIdHeader(),
        wwwAuthenticateHeader = findWWWAuthenticateHeader()?.parseToWWWAuthenticateHeader()
    )
}