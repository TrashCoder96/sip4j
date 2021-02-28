package ru.stech

import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.options.branchRegexp
import ru.stech.obj.ro.options.tagRegexp
import ru.stech.obj.ro.register.WWWAuthenticateHeader
import ru.stech.util.md5

interface Operation {

    fun isCompleted(): Boolean

    fun start()

    fun processReceivedBody(body: String)

    fun getResponseHash(method: SipMethod,
                        cnonce: String,
                        nc: String,
                        wwwAuthenticateHeader: WWWAuthenticateHeader,
                        sipClientProperties: SipClientProperties): String {
        val ha1 = md5("${sipClientProperties.user}:${wwwAuthenticateHeader.realm}:${sipClientProperties.password}")
        val ha2 = md5("${method.name}:sip:${sipClientProperties.serverIp};transport=UDP")
        return md5("${ha1}:${wwwAuthenticateHeader.nonce}:${nc}:${cnonce}:${wwwAuthenticateHeader.qop}:${ha2}")
    }

    fun extractBranchFromReceivedBody(body: String): String {
        val result = branchRegexp.find(body)
        return result!!.groupValues[1]
    }

    fun extractTagFromReceivedBody(body: String): String {
        val result = tagRegexp.find(body)
        return result!!.groupValues[1]
    }

}