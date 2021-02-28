package ru.stech.obj.ro.register

data class SipAuthorizationHeader(
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
)

fun SipAuthorizationHeader.buildString(): String {
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

