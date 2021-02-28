package ru.stech.obj.ro

data class SipContactHeader(
    val user: String,
    val localIp: String,
    val localPort: Int
)

fun SipContactHeader.buildString(): String {
    return "Contact: <sip:${user}@${localIp}:${localPort};transport=UDP>"
}