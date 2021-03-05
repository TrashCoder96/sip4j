package ru.stech.obj.ro.register

import ru.stech.obj.ro.SipObject

class SipAuthorizationHeader(
    val user: String,
    val realm: String,
    val nonce: String,
    val serverIp: String,
    val response: String,
    val cnonce: String,
    val nc: String,
    val qop: String,
    val algorithm: String,
    val opaque: String
): SipObject {
    override fun buildString(): String {
        return "Authorization: Digest username=\"${user}\"," +
                "realm=\"${realm}\"," +
                "nonce=\"${nonce}\"," +
                "uri=\"sip:${serverIp};transport=UDP\"," +
                "response=\"${response}\"," +
                "cnonce=\"${cnonce}\"," +
                "nc=${nc}," +
                "qop=${qop}," +
                "algorithm=${algorithm}," +
                "opaque=\"${opaque}\"\n"
    }
}
