package ru.stech.obj.ro

val sip20Regexp = Regex("SIP/2.0 ([0-9]+) (.*)")

abstract class SipResponse(
    val status: SipStatus,
    val viaHeader: SipViaHeader,
    val fromHeader: SipFromHeader,
    val toHeader: SipToHeader,
    val cSeqHeader: CSeqHeader,
    val callIdHeader: CallIdHeader,
): SipObject