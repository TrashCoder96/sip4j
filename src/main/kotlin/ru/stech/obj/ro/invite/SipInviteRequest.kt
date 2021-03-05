package ru.stech.obj.ro.invite

import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipRequest
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader
import ru.stech.obj.ro.register.SipAuthorizationHeader

class SipInviteRequest(
    viaHeader: SipViaHeader,
    toHeader: SipToHeader,
    fromHeader: SipFromHeader,
    contactHeader: SipContactHeader,
    cSeqHeader: CSeqHeader,
    callIdHeader: CallIdHeader,
    maxForwards: Int,
    val rtpPort: Int,
    val authorizationHeader: SipAuthorizationHeader?
): SipRequest(viaHeader, toHeader, fromHeader, contactHeader, cSeqHeader, callIdHeader, maxForwards) {

    override fun buildString(): String {
        return "INVITE sip:${toHeader.user}@${toHeader.user};transport=UDP SIP/2.0\n" +
                "${viaHeader.buildString()}\n" +
                "Max-Forwards: ${maxForwards}\n" +
                "${contactHeader.buildString()}\n" +
                "${toHeader.buildString()}\n" +
                "${fromHeader.buildString()}\n" +
                "${callIdHeader.buildString()}\n" +
                "${cSeqHeader.buildString()}\n" +
                "Allow: INVITE, ACK, CANCEL, BYE, NOTIFY, REFER, MESSAGE, OPTIONS, INFO, SUBSCRIBE\n" +
                "Content-Type: application/sdp\n" +
                "User-Agent: Sip4j Library\n" +
                (authorizationHeader?.buildString() ?: "") +
                "Allow-Events: presence, kpml, talk\n" +
                "Content-Length: 0\n" +
                "\n" +
                "v=0\n" +
                "o=Z 1614591671774 1 IN IP4 ${contactHeader.host}\n" +
                "s=Z\n" +
                "c=IN IP4 ${contactHeader.host}\n" +
                "t=0 0\n" +
                "m=audio $rtpPort RTP/AVP 106 9 98 101 0 8 3\n" +
                "a=rtpmap:106 opus/48000/2\n" +
                "a=fmtp:106 sprop-maxcapturerate=16000; minptime=20; useinbandfec=1\n" +
                "a=rtpmap:98 telephone-event/48000\n" +
                "a=fmtp:98 0-16\n" +
                "a=rtpmap:101 telephone-event/8000\n" +
                "a=fmtp:101 0-16\n" +
                "a=sendrecv"
    }

}
