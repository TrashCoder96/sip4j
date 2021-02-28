package ru.stech.obj.ro.register

import ru.stech.obj.ro.SipStatus

const val SIP20 = "SIP/2.0"
val sip20Regexp = Regex("SIP/2.0 (.*?)")

const val CALL_ID = "Call-ID:"

const val WWW_AUTH = "WWW-Authenticate:"
val realmRegexp = Regex("realm=\"(.*?)\"")
val nonceRegexp = Regex("nonce=\"(.*?)\"")
val opaqueRegexp = Regex("opaque=\"(.*?)\"")
val authRegexp = Regex("qop=\"(.*?)\"")

data class SipRegisterResponse(
    val status: SipStatus?,
    val callId: String?,
    val wwwAuthenticateHeader: WWWAuthenticateHeader?
)

fun String.parseToSipRegisterResponse(): SipRegisterResponse {
    var status: SipStatus? = null
    var callId: String? = null
    var wwwAuthenticateHeader: WWWAuthenticateHeader? = null
    for (line in this.lines()) {
        if (line.startsWith(SIP20)) {
            val parts = line.split(" ")
            status = SipStatus.valueOf(parts[2])
        }
        if (line.startsWith(CALL_ID)) {
            callId = line.substring(9, line.length - 1)
        }
        if (line.startsWith(WWW_AUTH)) {
            val realmMatchResult = realmRegexp.find(line)
            val nonceMatchResult = nonceRegexp.find(line)
            val opaqueMatchResult = opaqueRegexp.find(line)
            val authMathResult = authRegexp.find(line)

            wwwAuthenticateHeader = WWWAuthenticateHeader(
                realm = realmMatchResult!!.groupValues[1],
                nonce = nonceMatchResult!!.groupValues[1],
                opaque = opaqueMatchResult!!.groupValues[1],
                qop = authMathResult!!.groupValues[1]
            )
        }
    }
    return SipRegisterResponse(status = status,
        callId = callId,
        wwwAuthenticateHeader = wwwAuthenticateHeader)
}