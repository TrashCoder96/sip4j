package ru.stech.obj.ro.invite

import ru.stech.obj.ro.SipStatus
import ru.stech.obj.ro.register.WWWAuthenticateHeader
import ru.stech.obj.ro.register.findNonce
import ru.stech.obj.ro.register.findOpaque
import ru.stech.obj.ro.register.findQop
import ru.stech.obj.ro.register.findRealm

data class SipInviteResponse(
    val status: SipStatus,
    val wwwAuthenticateHeader: WWWAuthenticateHeader? = null
)

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