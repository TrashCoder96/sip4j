package ru.stech.obj.ro

private val responseURIHeaderRegexp = Regex("SIP/2.0 ([0-9]+) (.*?)[\n\r]")

enum class SipStatus(val status: Int) {
    Unauthorized(403),
    Forbidden(401),
    Busy(486),
    OK(200),
    Bad(400),
    Trying(100),
    Ringing(180)
}

fun String.findResponseURIHeader(): String {
    val result = responseURIHeaderRegexp.find(this) ?: throw SipParseException()
    return result.value
}

fun String.parseResponseSipStatus(): SipStatus {
    val result = responseURIHeaderRegexp.find(this) ?: throw SipParseException()
    val size = result.groupValues.size
    if (size > 2) {

    }
    return SipStatus.OK
}