package ru.stech.obj.ro

private val cSeqHeaderRegex = Regex("CSeq: (.*?) (.*?)\r\n")

class CSeqHeader(
    val cSeqNumber: Int,
    val method: SipMethod
): SipObject {
    override fun buildString(): String {
        return "CSeq: $cSeqNumber ${method.name}"
    }
}

fun String.findCSeqHeader(): String {
    val result = cSeqHeaderRegex.find(this) ?: throw SipParseException()
    return result.value
}

fun String.parseToCSeqHeader(): CSeqHeader {
    val result = cSeqHeaderRegex.find(this)
    return CSeqHeader(
        cSeqNumber = Integer.parseInt(result!!.groupValues[1]),
        method = SipMethod.valueOf(result.groupValues[2])
    )
}