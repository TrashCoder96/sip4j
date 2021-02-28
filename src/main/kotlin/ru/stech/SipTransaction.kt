package ru.stech

import java.util.*

class SipTransaction {
    val callId = UUID.randomUUID().toString()
    var number = 1
    fun nextNumber() = number++
}