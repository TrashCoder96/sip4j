package ru.stech.obj

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID

class RequestBuilder(
    private val serverHost: String
) {
    private var localHost: String = findIp()
    private var method: String? = null
    private var via: String? = null
    private var maxForward: Int? = null
    private var contact: String? = null
    private var to: String? = null
    private var from: String? = null
    private var callId: String? = null
    private var cseq: String? = null
    private var expires: Int? = null
    private var authorization: String? = null

    fun setMethod(method: String): RequestBuilder {
        this.method = method
        return this
    }

    fun setVia(localPort: Int): RequestBuilder {
        val branchId = "z9hG4bK_${UUID.randomUUID()}"
        this.via = "Via: SIP/2.0/UDP ${localHost}:${localPort};branch=${branchId};rport"
        return this
    }

    private fun findIp(): String {
        for (addr in NetworkInterface.getByName("vpn0").inetAddresses) {
            if (addr is Inet4Address) {
                return addr.hostAddress ?: ""
            }
        }
        return ""
    }

    fun setMaxForward(size: Int): RequestBuilder {
        this.maxForward = size
        return this
    }

    fun setContact(user: String, localPort: Int): RequestBuilder {
        this.contact = "Contact: <sip:${user}@${localHost}:${localPort};rinstance=8088b7bec3541f7l;transport=UDP>"
        return this
    }

    fun setToHeader(user: String): RequestBuilder {
        this.to = "To: <sip:${user}@${serverHost};transport=UDP>"
        return this
    }

    fun setFromHeader(user: String, tag: String = ""): RequestBuilder {
        this.from = "From: <sip:${user}@${serverHost};transport=UDP>"
        if (tag.isNotEmpty()) {
            this.from += ";tag=${tag}"
        }
        return this
    }

    fun setCallId(callId: String): RequestBuilder {
        this.callId = "Call-ID: $callId"
        return this
    }

    fun setCSeq(order: Int, method: String): RequestBuilder {
        this.cseq = "CSeq: $order $method"
        return this
    }

    fun setExpires(expires: Int): RequestBuilder {
        this.expires = expires
        return this
    }

    fun setAuthorization(user: String, password: String): RequestBuilder {
        this.authorization = "Authorization: " +
                "Digest username=\"${user}\"," +
                "realm=\"asterisk\"," +
                "nonce=\"1614237169/359588fe38b740f35d0fe8a8c03bf2fc\"," +
                "uri=\"sip:${serverHost};" +
                "transport=UDP\"," +
                "response=\"3f612ce96fff9633b92bf1361b01d5bf\"," +
                "cnonce=\"9420476cd5d67cecd1b93f0f049fc5fd\"," +
                "nc=00000001," +
                "qop=auth," +
                "algorithm=md5," +
                "opaque=\"6bb435532db6e150\""
        return this
    }

    fun build(): String {
        return "$method sip:${serverHost};transport=UDP SIP/2.0\n" +
                "$via\n" +
                "Max-Forwards: $maxForward\n" +
                "$contact\n" +
                "$to\n" +
                "$from\n" +
                "$callId\n" +
                "$cseq\n" +
                "Expires: $expires\n" +
                "Allow: INVITE, ACK, CANCEL, BYE, NOTIFY, REFER, MESSAGE, OPTIONS, INFO, SUBSCRIBE\n" +
                "User-Agent: Vera\n" +
                (authorization?:"") +
                "Allow-Events: presence, kpml, talk\n" +
                "Content-Length: 0\n" +
                "\n"
    }


}