package ru.stech.obj.ro.register

import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipResponse
import ru.stech.obj.ro.SipStatus
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader
import ru.stech.obj.ro.findCSeqHeader
import ru.stech.obj.ro.findFromHeaderLine
import ru.stech.obj.ro.findToHeaderLine
import ru.stech.obj.ro.findViaHeaderLine
import ru.stech.obj.ro.parseToCSeqHeader
import ru.stech.obj.ro.parseToCallIdHeader
import ru.stech.obj.ro.parseToFromHeader
import ru.stech.obj.ro.parseToToHeader
import ru.stech.obj.ro.parseToViaHeader

val sip20Regexp = Regex("SIP/2.0 ([0-9]+) (.*)")

class SipRegisterResponse(
    status: SipStatus,
    viaHeader: SipViaHeader,
    fromHeader: SipFromHeader,
    toHeader: SipToHeader,
    cSeqHeader: CSeqHeader,
    callIdHeader: CallIdHeader,
    val wwwAuthenticateHeader: WWWAuthenticateHeader?,
): SipResponse(status, viaHeader, fromHeader, toHeader, cSeqHeader, callIdHeader) {
    override fun buildString(): String {
        TODO("Not yet implemented")
    }
}

fun String.parseToSipRegisterResponse(): SipRegisterResponse {
    val sip20Result = sip20Regexp.find(this)
    val viaHeaderLine = this.findViaHeaderLine()
    val fromHeaderLine = this.findFromHeaderLine()
    val toHeaderLine = this.findToHeaderLine()
    val cSeqHeaderLine = this.findCSeqHeader()
    val callIdHeaderLine = this.findCSeqHeader()
    val wwwAuthenticateHeaderLine = this.findWWWAuthenticateHeader()
    return SipRegisterResponse(
        status = SipStatus.valueOf(sip20Result!!.groupValues[2]),
        viaHeader = viaHeaderLine!!.parseToViaHeader(),
        fromHeader = fromHeaderLine!!.parseToFromHeader(),
        toHeader = toHeaderLine!!.parseToToHeader(),
        cSeqHeader = cSeqHeaderLine!!.parseToCSeqHeader(),
        callIdHeader = callIdHeaderLine!!.parseToCallIdHeader(),
        wwwAuthenticateHeader = if (wwwAuthenticateHeaderLine != null) this.parseToWWWAuthenticateHeader() else null
    )
}