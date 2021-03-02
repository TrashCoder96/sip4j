package ru.stech

interface ServerOperation {

    fun askRequest(branch: String, request: String)
}