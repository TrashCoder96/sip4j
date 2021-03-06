package ru.stech.obj.ro

import kotlin.streams.asStream

private val contactHeader = Regex("Contact: <sip:(.*?)@(.*?):([0-9]+)(.*?)>(.*?)[\n\r]")
private val paramReg = Regex("([a-zA-Z0-9]+)=(.*?)[;>\n\r]")

class SipContactHeader(
    val user: String,
    val host: String,
    val port: Int,
    val hostParamsMap: Map<String, String> = mapOf(),
    val contactParamsMap: Map<String, String> = mapOf()
): SipObject {
    override fun buildString(): String {
        val hostParams = hostParamsMap.entries.joinToString(separator = "") {
            ";${it.key}=${it.value}"
        }
        val fromParams = contactParamsMap.entries.joinToString(separator = "") {
            ";${it.key}=${it.value}"
        }
        return "Contact: <sip:${user}@${host}:${port}${hostParams}>${fromParams}"
    }
}

fun String.findContactHeaderLine(): String {
    val result = contactHeader.find(this) ?: throw SipParseException()
    return result.value
}

fun String.parseToContactHeader(): SipContactHeader {
    val result = contactHeader.find(this)
    val hostParams = result!!.groupValues[4]
    val hostParamsMap = mutableMapOf<String, String>()
    paramReg.findAll(hostParams).asStream().forEach {
        hostParamsMap[it.groupValues[1]] = it.groupValues[2]
    }
    val contactParams = result.groupValues[5]
    val contactParamsMap = mutableMapOf<String, String>()
    paramReg.findAll(contactParams).asStream().forEach {
        contactParamsMap[it.groupValues[1]] = it.groupValues[2]
    }
    return SipContactHeader(
        user = result.groupValues[1],
        host = result.groupValues[2],
        port = Integer.parseInt(result.groupValues[3]),
        hostParamsMap = hostParamsMap,
        contactParamsMap = contactParamsMap
    )
}