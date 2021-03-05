package ru.stech.obj.ro.register

import ru.stech.obj.ro.SipObject

val wwwAuthorizationHeaderRegexp =
    Regex("WWW-Authenticate:(.*?)realm=\"(.*?)\",nonce=\"(.*?)\",opaque=\"(.*?)\",algorithm=(.*?),qop=\"(.*?)\"[\r\n;]")

class WWWAuthenticateHeader(
    val realm: String,
    val nonce: String,
    val opaque: String,
    val algorithm: String,
    val qop: String
): SipObject {
    override fun buildString(): String {
        TODO("Not yet implemented")
    }

}

fun String.findWWWAuthenticateHeader(): String? {
    return wwwAuthorizationHeaderRegexp.find(this)?.value
}

fun String.parseToWWWAuthenticateHeader(): WWWAuthenticateHeader {
    val result = wwwAuthorizationHeaderRegexp.find(this)
    return WWWAuthenticateHeader(
        realm = result!!.groupValues[2],
        nonce = result.groupValues[3],
        opaque = result.groupValues[4],
        algorithm = result.groupValues[5],
        qop = result.groupValues[6]
    )
}