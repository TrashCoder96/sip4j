package ru.stech.obj.ro

const val userAgent = "Sip4j Library"

interface SipObject {
    fun buildString(): String
}