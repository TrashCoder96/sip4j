package ru.stech

import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.register.WWWAuthenticateHeader
import ru.stech.util.md5

interface Transaction {

    fun start()

    fun processReceivedBody(branch: String, body: String)

    fun getResponseHash(cnonce: String,
                        nc: String,
                        wwwAuthenticateHeader: WWWAuthenticateHeader,
                        sipClientProperties: SipClientProperties): String {
        val ha1 = md5("${sipClientProperties.user}:${wwwAuthenticateHeader.realm}:${sipClientProperties.password}")
        val ha2 = md5("${SipMethod.REGISTER.name}:sip:${sipClientProperties.serverIp};transport=UDP")
        return md5("${ha1}:${wwwAuthenticateHeader.nonce}:${nc}:${cnonce}:${wwwAuthenticateHeader.qop}:${ha2}")
    }

}