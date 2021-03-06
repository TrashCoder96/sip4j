package ru.stech

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.SipStatus
import ru.stech.obj.ro.invite.parseToInviteResponse
import ru.stech.obj.ro.options.parseToOptionsRequest

class OptionsTests {

    @Test
    fun textToRequestOk() {
        val body = "OPTIONS sip:4090@192.168.188.159:55322;rinstance=ec0f53d3a1b16700 SIP/2.0\n" +
                "Via: SIP/2.0/UDP 10.255.250.29:5060;rport;branch=z9hG4bKPjcf9b8d17-fca2-419f-8f2a-954b55c350a8\n" +
                "From: <sip:4090@10.255.250.29>;tag=e4a232e9-d873-484c-a431-dd7ea9aa4933\n" +
                "To: <sip:4090@192.168.188.159;rinstance=ec0f53d3a1b16700>\n" +
                "Contact: <sip:4090@10.255.250.29:5060>\n" +
                "Call-ID: c8f7c2ce-9ac2-4d78-a0aa-30f01c0e5551\n" +
                "CSeq: 8878 OPTIONS\n" +
                "Max-Forwards: 70\n" +
                "User-Agent: Asterisk PBX 15.5.0\n" +
                "Content-Length:  0"
        val request = body.parseToOptionsRequest()
        assertEquals(SipMethod.OPTIONS, request.requestURIHeader.method)
        assertEquals("4090", request.requestURIHeader.user)
        assertEquals("192.168.188.159", request.requestURIHeader.host)
        assertEquals(55322, request.requestURIHeader.port)
        assertEquals("ec0f53d3a1b16700", request.requestURIHeader.hostParamsMap["rinstance"])
        assertEquals("10.255.250.29", request.viaHeader.host)
        assertEquals(5060, request.viaHeader.port)
        assertEquals("", request.viaHeader.hostParams["rport"])
        assertEquals("z9hG4bKPjcf9b8d17-fca2-419f-8f2a-954b55c350a8", request.viaHeader.hostParams["branch"])
        assertEquals("4090", request.fromHeader.user)
        assertEquals("10.255.250.29", request.fromHeader.host)
        assertEquals("e4a232e9-d873-484c-a431-dd7ea9aa4933", request.fromHeader.fromParamsMap["tag"])
        assertEquals("4090", request.toHeader.user)
        assertEquals("192.168.188.159", request.toHeader.host)
        assertEquals("ec0f53d3a1b16700", request.toHeader.hostParamsMap["rinstance"])
        assertEquals("4090", request.contactHeader.user)
        assertEquals("10.255.250.29", request.contactHeader.host)
        assertEquals(5060, request.contactHeader.port)
        assertEquals("c8f7c2ce-9ac2-4d78-a0aa-30f01c0e5551", request.callIdHeader.callId)
        assertEquals(8878, request.cSeqHeader.cSeqNumber)
        assertEquals(SipMethod.OPTIONS, request.cSeqHeader.method)
        assertEquals(70, request.maxForwards)
    }

    @Test
    fun textToResponse() {
        val body = "SIP/2.0 401 Unauthorized\n" +
                "Via: SIP/2.0/UDP 192.168.188.138:46285;rport=46285;received=192.168.188.138;branch=z9hG4bK-524287-1---40a1ba1bdb0b2fb9\n" +
                "Call-ID: I517YvNjae-wUTAm9TmrVQ..\n" +
                "From: <sip:4091@10.255.250.29>;tag=cad4fa61\n" +
                "To: <sip:4090@10.255.250.29>;tag=z9hG4bK-524287-1---40a1ba1bdb0b2fb9\n" +
                "CSeq: 1 INVITE\n" +
                "WWW-Authenticate: Digest  realm=\"asterisk\",nonce=\"1614591671/8588c0ebb4ca3345a06d55b661e63628\",opaque=\"4e89731c51d78736\",algorithm=md5,qop=\"auth\"\n" +
                "Server: Asterisk PBX 15.5.0\n" +
                "Content-Length:  0"
        val response = body.parseToInviteResponse()
        assertEquals(SipStatus.Unauthorized, response.status)
        assertEquals("192.168.188.138", response.viaHeader.host)
        assertEquals(46285, response.viaHeader.port)
        assertEquals("46285", response.viaHeader.hostParams["rport"])
        assertEquals("192.168.188.138", response.viaHeader.hostParams["received"])
        assertEquals("z9hG4bK-524287-1---40a1ba1bdb0b2fb9", response.viaHeader.hostParams["branch"])
        assertEquals("I517YvNjae-wUTAm9TmrVQ..", response.callIdHeader.callId)
        assertEquals("4091", response.fromHeader.user)
        assertEquals("10.255.250.29", response.fromHeader.host)
        assertEquals("cad4fa61", response.fromHeader.fromParamsMap["tag"])
        assertEquals("4090", response.toHeader.user)
        assertEquals("10.255.250.29", response.toHeader.host)
        assertEquals("z9hG4bK-524287-1---40a1ba1bdb0b2fb9", response.toHeader.toParamsMap["tag"])
        assertEquals(1, response.cSeqHeader.cSeqNumber)
        assertEquals(SipMethod.INVITE, response.cSeqHeader.method)
        assertEquals("asterisk", response.wwwAuthenticateHeader?.realm)
        assertEquals("1614591671/8588c0ebb4ca3345a06d55b661e63628", response.wwwAuthenticateHeader?.nonce)
        assertEquals("4e89731c51d78736", response.wwwAuthenticateHeader?.opaque)
        assertEquals("md5", response.wwwAuthenticateHeader?.algorithm)
        assertEquals("auth", response.wwwAuthenticateHeader?.qop)
    }
}