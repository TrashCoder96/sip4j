package ru.stech.obj.ro

private val fromHeaderRegex = Regex("From: <sip:(.*?)@(.*?)>(.*?)[\n\r]")

class SipFromHeader(
    val user: String,
    val host: String,
    val hostParamsMap: Map<String, String> = mapOf(),
    val fromParamsMap: Map<String, String> = mapOf()
): SipObject {

    override fun buildString(): String {
        val hostParams = hostParamsMap.entries.joinToString(separator = "") {
            ";${it.key}=${it.value}"
        }
        val fromParams = fromParamsMap.entries.joinToString(separator = "") {
            ";${it.key}=${it.value}"
        }
        return "From: <sip:${user}@${host}${hostParams}>${fromParams}"
    }
}

fun String.findFromHeaderLine(): String {
    val result = fromHeaderRegex.find(this) ?: throw SipParseException()
    return result.value
}

fun String.parseToFromHeader(): SipFromHeader {
    val result = fromHeaderRegex.find(this)
    val hostWithParams = result!!.groupValues[2]
    val hostAndParamsArray = hostWithParams.split(";")
    val host = hostAndParamsArray[0]
    val hostParamsMap = linkedMapOf<String, String>()
    for (i in 1 until hostAndParamsArray.size) {
        val keyAndValue = hostAndParamsArray[i].split("=")
        hostParamsMap[keyAndValue[0]] = keyAndValue[1]
    }
    val fromParams = result.groupValues[3]
    val fromParamsDevidedByColon = fromParams.split(";")
    val fromParamsMap = linkedMapOf<String, String>()
    for (i in 1 until fromParamsDevidedByColon.size) {
        val keyAndValue = fromParamsDevidedByColon[i].split("=")
        val key = keyAndValue[0]
        val value = if (keyAndValue.size > 1) keyAndValue[1] else ""
        fromParamsMap[key] = value
    }
    return SipFromHeader(
        user = result.groupValues[1],
        host = host,
        hostParamsMap = hostParamsMap,
        fromParamsMap = fromParamsMap
    )
}
