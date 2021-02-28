package ru.stech.util

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