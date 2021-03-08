package ru.stech.obj.ro.invite

import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipRequest
import ru.stech.obj.ro.SipRequestURIHeader
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader
import ru.stech.obj.ro.register.SipAuthorizationHeader
import ru.stech.obj.ro.userAgent

class SipInviteRequest(
    requestURIHeader: SipRequestURIHeader,
    viaHeader: SipViaHeader,
    toHeader: SipToHeader,
    fromHeader: SipFromHeader,
    val contactHeader: SipContactHeader,
    cSeqHeader: CSeqHeader,
    callIdHeader: CallIdHeader,
    maxForwards: Int,
    val authorizationHeader: SipAuthorizationHeader? = null,
    val rtpPort: Int,
): SipRequest(requestURIHeader, viaHeader, toHeader, fromHeader, cSeqHeader, callIdHeader, maxForwards) {

    override fun buildString(): String {
        val messageBodyByteArray = "v=0\r\n" +
                "o=Z 1614591671774 1 IN IP4 ${contactHeader.host}\r\n" +
                "s=Z\r\n" +
                "c=IN IP4 ${contactHeader.host}\r\n" +
                "t=0 0\r\n" +
                "m=audio $rtpPort RTP/AVP 106 9 98 101 0 8 3\r\n" +
                "a=rtpmap:106 opus/48000/2\r\n" +
                "a=fmtp:106 sprop-maxcapturerate=16000; minptime=20; useinbandfec=1\r\n" +
                "a=rtpmap:98 telephone-event/48000\r\n" +
                "a=fmtp:98 0-16\r\n" +
                "a=rtpmap:101 telephone-event/8000\r\n" +
                "a=fmtp:101 0-16\r\n" +
                "a=sendrecv".toByteArray()

        return "${requestURIHeader.buildString()}\r\n" +
                "${viaHeader.buildString()}\r\n" +
                "${contactHeader.buildString()}\r\n" +
                "${toHeader.buildString()}\r\n" +
                "${fromHeader.buildString()}\r\n" +
                "${callIdHeader.buildString()}\r\n" +
                "${cSeqHeader.buildString()}\r\n" +
                "Max-Forwards: ${maxForwards}\r\n" +
                "Allow: INVITE, ACK, CANCEL, BYE, NOTIFY, REFER, MESSAGE, OPTIONS, INFO, SUBSCRIBE\r\n" +
                "Content-Type: application/sdp\r\n" +
                "User-Agent: ${userAgent}\r\n" +
                (authorizationHeader?.buildString() ?: "") +
                "Allow-Events: presence, kpml, talk\r\n" +
                "Content-Length: ${messageBodyByteArray.length}\r\n" +
                "\n" +
                "v=0\r\n" +
                "o=Z 1614591671774 1 IN IP4 ${contactHeader.host}\r\n" +
                "s=Z\r\n" +
                "c=IN IP4 ${contactHeader.host}\r\n" +
                "t=0 0\r\n" +
                "m=audio $rtpPort RTP/AVP 106 9 98 101 0 8 3\r\n" +
                "a=rtpmap:106 opus/48000/2\r\n" +
                "a=fmtp:106 sprop-maxcapturerate=16000; minptime=20; useinbandfec=1\r\n" +
                "a=rtpmap:98 telephone-event/48000\r\n" +
                "a=fmtp:98 0-16\r\n" +
                "a=rtpmap:101 telephone-event/8000\r\n" +
                "a=fmtp:101 0-16\r\n" +
                "a=sendrecv"
    }

}
