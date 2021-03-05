package ru.stech.obj.ro

private val requestURIHeaderRegexp = Regex("(.*?) sip:(.*?) SIP/2.0")
private val userRegexp = Regex("sip:(.*?)@")
private val hostParamsRegexp = Regex(";(.*?) SIP/2.0")
private val portRegexp = Regex(":([0-9]+)")

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
            ";${it.value}=${it.value}"
        }
        return "${method.name} sip:${if (user != null) "${user}@" else ""}${host}${if (port != null) "$port" else ""}${hostParams}"
    }
}

fun String.findRequestURIHeader(): String? {
    return requestURIHeaderRegexp.find(this)?.value
}

fun String.parseToSipRequestURIHeader(): SipRequestURIHeader {
    val method = requestURIHeaderRegexp.find(this)!!.groupValues[1]
    val user = userRegexp.find(this)?.value
    val port = portRegexp.find(this)?.value
    val host = findHost(this)
    return SipRequestURIHeader(
        method = SipMethod.valueOf(method),
        user = user,
        host = host!!,
        port = Integer.parseInt(port),
        hostParamsMap = extractHostParamsMap(this)
    )
}

private fun extractHostParamsMap(line: String): Map<String, String> {
    val hostParamsMap = mutableMapOf<String, String>()
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

private fun findHost(line: String): String? {
    val result1 = hostRegexp1.find(line)
    var host: String? = null
    if (result1?.groupValues?.size ?: 0 > 1) {
        host = result1!!.groupValues[1]
    }
    val result2 = hostRegexp2.find(line)
    if (result2?.groupValues?.size ?: 0 > 1) {
        host = result2!!.groupValues[1]
    }
    val result3 = hostRegexp3.find(line)
    if (result3?.groupValues?.size ?: 0 > 1) {
        host = result3!!.groupValues[1]
    }
    return host
}