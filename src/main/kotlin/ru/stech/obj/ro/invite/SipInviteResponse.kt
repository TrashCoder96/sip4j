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

}