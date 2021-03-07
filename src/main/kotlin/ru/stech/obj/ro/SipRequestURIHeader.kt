package ru.stech.obj.ro

private val requestURIHeaderRegexp = Regex("(.*?) sip:(.*?) SIP/2.0")
private val userRegexp = Regex("sip:(.*?)@")
private val hostParamsRegexp = Regex(";(.*?) SIP/2.0")
private val portRegexp = Regex(":([0-9]+)[;\\s]")

private val hostRegexp1 = Regex("@(.*?):")
private val hostRegexp2 = Regex("sip:(.*?):")
private val hostRegexp3 = Regex("sip:(.*?) SIP/2.0")

class SipRequestURIHeader(
    val method: SipMethod,
    val user: String? = null,
    val host: String,
    val port: Int? = null,
    val hostParamsMap: Map<String, String> = mapOf()
): SipObject {
    override fun buildString(): String {
        val hostParams = hostParamsMap.entries.joinToString(separator = "") {
            ";${it.key}=${it.value}"
        }
        return "${method.name} sip:${if (user != null) "${user}@" else ""}${host}${if (port != null) "$port" else ""}${hostParams} SIP/2.0"
    }
}

fun String.findRequestURIHeader(): String? {
    return requestURIHeaderRegexp.find(this)?.value
}

fun String.parseToSipRequestURIHeader(): SipRequestURIHeader {
    val method = parseMethod(this)
    val user = parseUser(this)
    val port = parsePort(this)
    val host = findHost(this)
    return SipRequestURIHeader(
        method = method,
        user = user,
        host = host,
        port = port,
        hostParamsMap = extractHostParamsMap(this)
    )
}

private fun parsePort(line: String): Int? {
    val portMatchResult = portRegexp.find(line) ?: throw SipParseException()
    return if (portMatchResult.groupValues.size > 1) {
        Integer.parseInt(portMatchResult.groupValues[1])
    } else {
        null
    }
}

private fun parseUser(line: String): String? {
    val userMatchResult = userRegexp.find(line) ?: throw SipParseException()
    return if (userMatchResult.groupValues.size > 1) {
        userMatchResult.groupValues[1]
    } else {
        null
    }
}

private fun parseMethod(line: String): SipMethod {
    val methodMatchResult = requestURIHeaderRegexp.find(line) ?: throw SipParseException()
    if (methodMatchResult.groupValues.size > 2) {
        return SipMethod.valueOf(methodMatchResult.groupValues[1])
    } else {
        throw SipParseException()
    }
}

private fun extractHostParamsMap(line: String): Map<String, String> {
    val hostParamsMap = linkedMapOf<String, String>()
    val hostParamsResult = hostParamsRegexp.find(line)
    if (hostParamsResult != null) {
        val hostParamsArrayDividedByColon = hostParamsResult.groupValues[1].split(";")
        for (i in hostParamsArrayDividedByColon.indices) {
            val keyAndValue = hostParamsArrayDividedByColon[i].split("=")
            val key = keyAndValue[0]
            val value = if (keyAndValue.isNotEmpty()) keyAndValue[1] else ""
            hostParamsMap[key] = value
        }
    }
    return hostParamsMap
}

private fun findHost(line: String): String {
    val result1 = hostRegexp1.find(line)
    var host: String? = null
    if (result1?.groupValues?.size ?: 0 > 1) {
        host = result1!!.groupValues[1]
    }
    if (host != null) return host
    val result2 = hostRegexp2.find(line)
    if (result2?.groupValues?.size ?: 0 > 1) {
        host = result2!!.groupValues[1]
    }
    if (host != null) return host
    val result3 = hostRegexp3.find(line)
    if (result3?.groupValues?.size ?: 0 > 1) {
        host = result3!!.groupValues[1]
    }
    return host ?: throw SipParseException()
}