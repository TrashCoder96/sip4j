package ru.stech.obj.ro

abstract class SipResponse(
    val status: SipStatus,
    val viaHeader: SipViaHeader,
    val fromHeader: SipFromHeader,
    val toHeader: SipToHeader,
    val cSeqHeader: CSeqHeader,
    val callIdHeader: CallIdHeader,
): SipObject