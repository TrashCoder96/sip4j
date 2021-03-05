package ru.stech.obj.ro

abstract class SipRequest(
    val viaHeader: SipViaHeader,
    val toHeader: SipToHeader,
    val fromHeader: SipFromHeader,
    val contactHeader: SipContactHeader,
    val cSeqHeader: CSeqHeader,
    val callIdHeader: CallIdHeader,
    val maxForwards: Int
): SipObject