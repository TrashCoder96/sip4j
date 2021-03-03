package ru.stech.obj.ro

data class SipFromHeader(
    val user: String,
    val host: String,
    val tag: String = ""
)

fun SipFromHeader.buildString(): String {
    val result = "From: <sip:${user}@${host}>"
    val tagString = if (tag.isNotBlank()) ";tag=${tag}" else ""
    return result + tagString
}
