package ru.stech.obj.ro

data class SipToHeader(
    val user: String,
    val host: String
)

fun SipToHeader.buildString(): String {
    return "To: <sip:${user}@${host};transport=UDP>"
}