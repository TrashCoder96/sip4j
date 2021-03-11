package ru.stech.rtp

import kotlinx.coroutines.Dispatchers

class RtpSessionFactory {
    fun newRtpSession(localPort: Int,
                      remoteRtpIp: String,
                      remoteRtpPort: Int): RtpSession {
        return RtpSession(
            localPort = localPort,
            remoteRtpIp = remoteRtpIp,
            remoteRtpPort = remoteRtpPort,
            dispatcher = Dispatchers.IO
        )
    }

}