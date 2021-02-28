package ru.stech.obj.ro

enum class SipStatus(val status: Int) {
    Unauthorized(403),
    Forbidden(401),
    OK(200),
    Bad(400),
    Trying(100),
    Ringing(180)
}