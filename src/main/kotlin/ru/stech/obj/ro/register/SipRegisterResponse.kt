package ru.stech.obj.ro.register

import ru.stech.obj.ro.SipStatus

const val SIP20 = "SIP/2.0"
val sip20Regexp = Regex("SIP/2.0 (.*?)")

const val CALL_ID = "Call-ID:"

const val WWW_AUTH = "WWW-Authenticate:"
val realmRegexp = Regex("realm=\"(.*?)\"")
fun findRealm(line: String): String {
    return realmRegexp.find(line)!!.groupValues[1]
}

val nonceRegexp = Regex("nonce=\"(.*?)\"")
fun findNonce(line: String): String {
    return nonceRegexp.find(line)!!.groupValues[1]
}

val opaqueRegexp = Regex("opaque=\"(.*?)\"")
fun findOpaque(line: String): String {
    return opaqueRegexp.find(line)!!.groupValues[1]
}

val qopRegexp = Regex("qop=\"(.*?)\"")
fun findQop(line: String): String {
    return qopRegexp.find(line)!!.groupValues[1]
}

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
            wwwAuthenticateHeader = WWWAuthenticateHeader(
                realm = findRealm(line),
                nonce = findNonce(line),
                opaque = findOpaque(line),
                qop = findQop(line)
            )
        }
    }
    return SipRegisterResponse(status = status,
        callId = callId,
        wwwAuthenticateHeader = wwwAuthenticateHeader)
}