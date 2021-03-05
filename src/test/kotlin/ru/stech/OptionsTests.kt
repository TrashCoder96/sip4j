package ru.stech

import org.junit.jupiter.api.Test
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

    }
}