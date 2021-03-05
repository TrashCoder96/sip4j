package ru.stech.obj.ro

private val viaHeaderRegexp = Regex("Via: (.*?)/(.*?) (.*?):([0-9]+)(.*?)[\n\r]")

class SipViaHeader(
    val host: String,
    val port: Int,
    val hostParams: Map<String, String> = mapOf()
): SipObject {
    override fun buildString(): String {
        val hostParams = hostParams.entries.joinToString(separator = "") {
            if (it.value == "") {
                ";${it.key}"
            } else {
                ";${it.key}=${it.value}"
            }
        }
        return "Via: SIP/2.0/UDP ${host}:${port}${hostParams}"
    }
}

fun String.findViaHeaderLine(): String {
    val result = viaHeaderRegexp.find(this) ?: throw SipParseException()
    return result.value
}

fun String.parseToViaHeader(): SipViaHeader {
    val result = viaHeaderRegexp.find(this)
    val hostParams = result!!.groupValues[4]
    val hostParamsMap = mutableMapOf<String, String>()
    val hostParamPartsDividedByColon = hostParams.split(";")
    for (i in 1 until hostParamPartsDividedByColon.size) {
        val keyAndValue = hostParamPartsDividedByColon[i].split("=")
        val key = keyAndValue[0]
        val value = if (keyAndValue.isNotEmpty()) keyAndValue[1] else ""
        hostParamsMap[key] = value
    }
    return SipViaHeader(
        host = result.groupValues[3],
        port = Integer.parseInt(result.groupValues[4]),
        hostParams = hostParamsMap
    )
}