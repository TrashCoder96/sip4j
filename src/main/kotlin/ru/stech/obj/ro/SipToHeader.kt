package ru.stech.obj.ro

import kotlin.streams.asStream

private val toHeaderRegex = Regex("To: <sip:(.*?)@(.*?)>(.*?)[\n\r]")
private val paramReg = Regex("([a-zA-Z0-9]+)=([a-zA-Z0-9]+)")

class SipToHeader(
    val user: String,
    val host: String,
    val hostParamsMap: Map<String, String> = mapOf(),
    val toParamsMap: Map<String, String> = mapOf()
): SipObject {

    override fun buildString(): String {
        val hostParams = hostParamsMap.entries.joinToString(separator = "") {
            ";${it.key}=${it.value}"
        }
        val toParams = toParamsMap.entries.joinToString(separator = "") {
            ";${it.key}=${it.value}"
        }
        return "To: <sip:${user}@${host}${hostParams}>${toParams}"
    }
}

fun String.findToHeaderLine(): String {
    val result = toHeaderRegex.find(this) ?: throw SipParseException()
    return result.value
}

fun String.parseToToHeader(): SipToHeader {
    val result = toHeaderRegex.find(this)
    val hostWithParams = result!!.groupValues[2]
    val hostAndParamsArray = hostWithParams.split(";")
    val host = hostAndParamsArray[0]
    val hostParamsMap = mutableMapOf<String, String>()
    for (i in 1 until hostAndParamsArray.size) {
        val keyAndValue = hostAndParamsArray[i].split("=")
        hostParamsMap[keyAndValue[0]] = keyAndValue[1]
    }
    val fromParams = result.groupValues[3]
    val fromParamsMap = mutableMapOf<String, String>()
    paramReg.findAll(fromParams).asStream().forEach {
        fromParamsMap[it.groupValues[1]] = it.groupValues[2]
    }
    return SipToHeader(
        user = result.groupValues[1],
        host = host,
        hostParamsMap = hostParamsMap,
        toParamsMap = fromParamsMap
    )
}