package ru.stech.obj.ro.invite

import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.register.SipAuthorizationHeader
import ru.stech.obj.ro.register.buildString

class SipInviteRequest(
    val branch: String,
    val contactHeader: SipContactHeader,
    val toHeader: SipToHeader,
    val fromHeader: SipFromHeader,
    val maxForwards: Int,
    val callId: String,
    val cseqNumber: Int,
    val authorizationHeader: SipAuthorizationHeader? = null) {

    fun buildString(): String {
        return "INVITE sip:${toHeader.user}@${toHeader.user};transport=UDP SIP/2.0\n" +
                "Via: SIP/2.0/UDP ${contactHeader.localIp}:${contactHeader.localPort};branch=${branch};rport\n" +
                "Max-Forwards: ${maxForwards}\n" +
                "Contact: <sip:${contactHeader.user}@${contactHeader.localIp}:${contactHeader.localPort};transport=UDP>\n" +
                "To: <sip:${toHeader.user}@${toHeader.host}>\n" +
                "From: <sip:${fromHeader.user}@${fromHeader.host};transport=UDP>;tag=cad4fa61\n" +
                "Call-ID: ${callId}\n" +
                "CSeq: $cseqNumber INVITE\n" +
                "Allow: INVITE, ACK, CANCEL, BYE, NOTIFY, REFER, MESSAGE, OPTIONS, INFO, SUBSCRIBE\n" +
                "Content-Type: application/sdp\n" +
                "User-Agent: Sip4j Library\n" +
                (authorizationHeader?.buildString() ?: "") +
                "Allow-Events: presence, kpml, talk\n" +
                "Content-Length: 0\n" +
                "\n" +
                "v=0\n" +
                "o=Z 1614591671774 1 IN IP4 ${contactHeader.localIp}\n" +
                "s=Z\n" +
                "c=IN IP4 ${contactHeader.localIp}\n" +
                "t=0 0\n" +
                "m=audio 8000 RTP/AVP 106 9 98 101 0 8 3\n" +
                "a=rtpmap:106 opus/48000/2\n" +
                "a=fmtp:106 sprop-maxcapturerate=16000; minptime=20; useinbandfec=1\n" +
                "a=rtpmap:98 telephone-event/48000\n" +
                "a=fmtp:98 0-16\n" +
                "a=rtpmap:101 telephone-event/8000\n" +
                "a=fmtp:101 0-16\n" +
                "a=sendrecv"
    }

}
