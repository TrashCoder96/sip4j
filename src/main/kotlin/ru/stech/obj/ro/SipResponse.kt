package ru.stech.obj.ro

data class SipResponse(
    val status: SipStatus,
    val callId: String,
    val from: String,
    val to: String,
)