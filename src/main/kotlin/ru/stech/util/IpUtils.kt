package ru.stech.util

import ru.stech.SipClientProperties
import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.options.branchRegexp
import ru.stech.obj.ro.register.WWWAuthenticateHeader
import java.math.BigInteger
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.MessageDigest

fun findIp(): String {
    for (addr in NetworkInterface.getByName("vpn0").inetAddresses) {
        if (addr is Inet4Address) {
            return addr.hostAddress ?: ""
        }
    }
    return ""
}

fun md5(input:String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
}

fun extractBranchFromReceivedBody(body: String): String {
    val result = branchRegexp.find(body)
    return result!!.groupValues[1]
}

fun getResponseHash(method: SipMethod,
                    cnonce: String,
                    nc: String,
                    wwwAuthenticateHeader: WWWAuthenticateHeader,
                    sipClientProperties: SipClientProperties
): String {
    val ha1 = md5("${sipClientProperties.user}:${wwwAuthenticateHeader.realm}:${sipClientProperties.password}")
    val ha2 = md5("${method.name}:sip:${sipClientProperties.serverIp};transport=UDP")
    return md5("${ha1}:${wwwAuthenticateHeader.nonce}:${nc}:${cnonce}:${wwwAuthenticateHeader.qop}:${ha2}")
}