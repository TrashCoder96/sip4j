package ru.stech.obj.ro.invite

import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipResponse
import ru.stech.obj.ro.SipStatus
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader
import ru.stech.obj.ro.register.WWWAuthenticateHeader

class SipInviteResponse(
    val wwwAuthenticateHeader: WWWAuthenticateHeader? = null,
    status: SipStatus,
    viaHeader: SipViaHeader,
    fromHeader: SipFromHeader,
    toHeader: SipToHeader,
    cSeqHeader: CSeqHeader,
    callIdHeader: CallIdHeader,
): SipResponse(status, viaHeader, fromHeader, toHeader, cSeqHeader, callIdHeader) {
    override fun buildString(): String {
        TODO("Not yet implemented")
    }

}

fun String.parseToInviteResponse(): SipInviteResponse {
    val lines = this.lines()
    val status = SipStatus.valueOf(lines[0].split(" ")[2])
    if (lines[6].startsWith("WWW-Authenticate:")) {
        val realm = findRealm(lines[6])
        val nonce = findNonce(lines[6])
        val opaque = findOpaque(lines[6])
        val qop = findQop(lines[6])
        val wwwAuthenticateHeader = WWWAuthenticateHeader(
            realm = realm,
            nonce = nonce,
            opaque = opaque,
            qop = qop
        )
        return SipInviteResponse(
            status = status,
            wwwAuthenticateHeader = wwwAuthenticateHeader
        )
    } else {
        return SipInviteResponse(
            status = status
        )
    }
}