package ru.stech.obj.ro.options

import ru.stech.obj.ro.SipMethod

val branchRegexp = Regex("branch=(.*)")
val tagRegexp = Regex("tag=(.*)")
val callIdRegexp = Regex("Call-ID: (.*)")

data class SipOptionsRequest(
    val method: SipMethod,
    val callId: String,
    val branch: String,
    val tag: String,
    val cseqNumber: Int
)

fun String.parseToOptionsRequest(): SipOptionsRequest {
    val lines = this.lines()
    val firstLineParts = lines[0].split(" ")
    val method = SipMethod.valueOf(firstLineParts[0])
    val matchBranchResult = branchRegexp.find(lines[1])
    val branch = matchBranchResult!!.groupValues[1]
    val tagMatchResult = tagRegexp.find(lines[2])
    val tag = tagMatchResult!!.groupValues[1]
    val cseqNumber = lines[7].split(" ")[1].toInt()

    val fivethLineParts = lines[5].split(" ")
    val callId = fivethLineParts[1]
    return SipOptionsRequest(
        method = method,
        callId = callId,
        branch = branch,
        tag = tag,
        cseqNumber = cseqNumber
    )
}