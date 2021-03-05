package ru.stech.obj.ro

private val callIdHeaderRegex = Regex("Call-ID: (.*?)\r\n")

class CallIdHeader(
    val callId: String
): SipObject {
    override fun buildString(): String {
        return "Call-ID: $callId"
    }
}

fun String.findCallIdHeader(): String? {
    return callIdHeaderRegex.find(this)?.value
}

fun String.parseToCallIdHeader(): CallIdHeader {
    val result = callIdHeaderRegex.find(this)
    return CallIdHeader(
        callId = result!!.groupValues[1]
    )
}