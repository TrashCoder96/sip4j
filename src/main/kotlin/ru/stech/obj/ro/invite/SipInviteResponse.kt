package ru.stech.obj.ro.invite

import ru.stech.obj.ro.SipStatus
import ru.stech.obj.ro.register.*

data class SipInviteResponse(
    val status: SipStatus,
    val wwwAuthenticateHeader: WWWAuthenticateHeader
)

fun String.parseToInviteResponse(): SipInviteResponse {
    val lines = this.lines()
    val status = SipStatus.valueOf(lines[0].split(" ")[2])
    val realm = findRealm(lines[6])
    val nonce = findNonce(lines[6])
    val opaque = findOpaque(lines[6])
    val qop = findQop(lines[6])
    return SipInviteResponse(
        status = status,
        WWWAuthenticateHeader(
            realm = realm,
            nonce = nonce,
            opaque = opaque,
            qop = qop
        )
    )
}