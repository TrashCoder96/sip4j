package ru.stech.obj.ro

abstract class SipRequest(
    val requestURIHeader: SipRequestURIHeader,
    val viaHeader: SipViaHeader,
    val toHeader: SipToHeader,
    val fromHeader: SipFromHeader,
    val cSeqHeader: CSeqHeader,
    val callIdHeader: CallIdHeader,
    val maxForwards: Int
): SipObject