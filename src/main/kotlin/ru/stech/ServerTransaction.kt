package ru.stech

interface ServerTransaction {

    fun askRequest(branch: String, request: String)
}