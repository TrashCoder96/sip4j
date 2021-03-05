package ru.stech.obj.ro

import kotlin.streams.asStream

private val fromHeaderRegex = Regex("From: <sip:(.*?)@(.*?)>(.*?)[\n\r]")
private val paramReg = Regex("([a-zA-Z0-9]+)=([a-zA-Z0-9]+)")

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

fun String.findFromHeaderLine(): String? {
    return fromHeaderRegex.find(this)?.value
}

fun String.parseToFromHeader(): SipFromHeader {
    val result = fromHeaderRegex.find(this)
    val hostWithParams = result!!.groupValues[2]
    val hostAndParamsArray = hostWithParams.split(";")
    val host = hostAndParamsArray[0]
    val hostParamsMap = mutableMapOf<String, String>()
    for (i in 1 .. hostAndParamsArray.size) {
        val keyAndValue = hostAndParamsArray[i].split("=")
        hostParamsMap[keyAndValue[0]] = keyAndValue[1]
    }
    val fromParams = result.groupValues[3]
    val fromParamsMap = mutableMapOf<String, String>()
    paramReg.findAll(fromParams).asStream().forEach {
        fromParamsMap[it.groupValues[1]] = it.groupValues[2]
    }
    return SipFromHeader(
        user = result.groupValues[1],
        host = host,
        hostParamsMap = hostParamsMap,
        fromParamsMap = fromParamsMap
    )
}
