package ru.stech.util

import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.SipParseException
import java.math.BigInteger
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.*

val maxForwardsRegex = Regex("Max-Forwards: ([0-9]+)")

val contactHeaderRegex = Regex("Contact: (.*?)\r\n")
val fromHeaderRegex = Regex("From: (.*?)\r\n")
val toHeaderRegex = Regex("To: (.*?)\r\n")

val methodRegexp = Regex("^(${SipMethod.values().joinToString("|")})")
val branchRegexp = Regex("branch=z9hG4bK(.*?)[^;\\n]+")
val callIdRegexp = Regex("Call-ID: (.*?)")
val tagRegexp = Regex("tag=(.*)")
val cseqNumberRegexp = Regex("CSeq: (.*?) ")


fun findIp(): String {
    for (addr in NetworkInterface.getByName("vpn0").inetAddresses) {
        if (addr is Inet4Address) {
            return addr.hostAddress ?: ""
        }
    }
    return ""
}

fun findMethod(body: String): String {
    val result = methodRegexp.find(body)
    return result!!.value
}

fun md5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
}

fun extractContactHeaderFromReceivedBody(body: String): String {
    return contactHeaderRegex.find(body)!!.value
}

fun extractFromHeaderfromReceivedBody(body: String): String {
    return fromHeaderRegex.find(body)!!.value
}

fun extractToHeaderFromReceivedBody(body: String): String {
    return toHeaderRegex.find(body)!!.value
}

fun findMaxForwards(body: String): Int {
    val result = maxForwardsRegex.find(body) ?: throw SipParseException()
    return Integer.parseInt(result.groupValues[1])
}

fun extractBranchFromReceivedBody(body: String): String {
    val result = branchRegexp.find(body)!!.value
    return result.substring(7).trim()
}

fun extractCallIdFromReceived(body: String): String {
    val result = callIdRegexp.find(body)!!
    return result.groupValues[1]
}

fun extractCSeqNumberFromReceivedBody(body: String): Int {
    val result = cseqNumberRegexp.find(body)!!
    return Integer.parseInt(result.groupValues[1])
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

fun randomString(targetStringLength: Int): String {
    val leftLimit = 48 // numeral '0'
    val rightLimit = 122 // letter 'z'
    val random = Random()
    return random.ints(leftLimit, rightLimit + 1)
        .filter { i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97) }
        .limit(targetStringLength.toLong())
        .collect({ StringBuilder() }, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString()
}