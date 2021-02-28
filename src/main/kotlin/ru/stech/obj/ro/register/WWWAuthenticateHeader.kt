package ru.stech.obj.ro.register

data class WWWAuthenticateHeader(
    val realm: String,
    val nonce: String,
    val opaque: String,
    val algorithm: String = "md5",
    val qop: String
)