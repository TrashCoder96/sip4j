package ru.stech.obj.ro

data class SipToHeader(
    val user: String,
    val host: String,
    val tag: String = ""
)

fun SipToHeader.buildString(): String {
    val result = "To: <sip:${user}@${host};transport=UDP>"
    val tagString = if (tag.isNotBlank()) ";tag=${tag}" else ""
    return result + tagString
}