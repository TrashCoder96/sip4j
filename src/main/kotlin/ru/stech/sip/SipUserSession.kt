package ru.stech.sip

import kotlinx.coroutines.channels.Channel
import ru.stech.obj.ro.bye.SipByeRequest
import ru.stech.obj.ro.invite.SipInviteResponse
import ru.stech.obj.ro.options.SipOptionsRequest
import ru.stech.obj.ro.register.SipRegisterResponse
import ru.stech.util.randomString
import java.util.*

class SipUserSession(private val to: String) {
    private val callId = UUID.randomUUID().toString()
    private var registerCallId = UUID.randomUUID().toString()
    private var registerNonce: String? = null
    private var registerRealm: String? = null
    private var registerNc = 0
    private var registerQop: String? = null
    private var registerAlgorithm: String? = null
    private var registerOpaque: String? = null

    private var rinstance = randomString(16)
    private var registerBranch = "z9hG4bK${UUID.randomUUID()}"
    private val registerResponseChannel = Channel<SipRegisterResponse>(0)

    private var inviteBranch = "z9hG4bK${UUID.randomUUID()}"
    private val inviteResponseChannel = Channel<SipInviteResponse>(0)

    private val optionsRequestChannel = Channel<SipOptionsRequest>(0)

    private val byeRequestChannel = Channel<SipByeRequest>(0)

    fun findAppropriateChannel(sipData: ByteArray) {

    }

}