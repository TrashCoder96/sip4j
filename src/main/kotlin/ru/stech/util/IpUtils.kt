package ru.stech.util

import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.options.branchRegexp
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
    val result = branchRegexp.find(body)!!.value
    return result.substring(7).trim()
}

fun getResponseHash(user: String,
                    realm: String,
                    password: String,
                    method: SipMethod,
                    serverIp: String,
                    nonce: String,
                    nc: String,
                    cnonce: String,
                    qop: String
): String {
    val ha1 = md5("${user}:${realm}:${password}")
    val ha2 = md5("${method.name}:sip:${serverIp};transport=UDP")
    return md5("${ha1}:${nonce}:${nc}:${cnonce}:${qop}:${ha2}")
}