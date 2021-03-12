package ru.stech.sip

import io.netty.channel.ChannelFuture
import io.netty.channel.epoll.EpollEventLoopGroup
import kotlinx.coroutines.channels.Channel
import ru.stech.obj.ro.CSeqHeader
import ru.stech.obj.ro.CallIdHeader
import ru.stech.obj.ro.SipContactHeader
import ru.stech.obj.ro.SipFromHeader
import ru.stech.obj.ro.SipMethod
import ru.stech.obj.ro.SipRequestURIHeader
import ru.stech.obj.ro.SipStatus
import ru.stech.obj.ro.SipToHeader
import ru.stech.obj.ro.SipViaHeader
import ru.stech.obj.ro.ack.SipAckRequest
import ru.stech.obj.ro.invite.SipInviteRequest
import ru.stech.obj.ro.invite.SipInviteResponse
import ru.stech.obj.ro.register.SipAuthorizationHeader
import ru.stech.util.getResponseHash
import ru.stech.util.randomString
import ru.stech.util.sendSipRequest
import java.util.UUID

class SipSession(val to: String,
                 val client: Client,
                 val callId: String,
                 private val inviteResponseChannel: Channel<SipInviteResponse>,
                 private val workerGroup: EpollEventLoopGroup,
                 private var inviteBranchRef: String? = null
) {
    private val sipClientProperties = client.sipClientProperties
    private var sipChannelFuture: ChannelFuture? = null

    suspend fun startCall() {
        val nc = "00000001"
        val fromTag = randomString(8)
        val inviteRequest = SipInviteRequest(
            requestURIHeader = SipRequestURIHeader(
                method = SipMethod.INVITE,
                user = to,
                host = sipClientProperties.serverIp,
                hostParamsMap = linkedMapOf(
                    "transport" to "UDP"
                )
            ),
            viaHeader = SipViaHeader(
                host = sipClientProperties.clientIp,
                port = sipClientProperties.clientPort,
                hostParams = linkedMapOf(
                    "branch" to inviteBranchRef!!
                )
            ),
            contactHeader = SipContactHeader(
                user = sipClientProperties.user,
                host = sipClientProperties.clientIp,
                port = sipClientProperties.clientPort
            ),
            toHeader = SipToHeader(
                user = to,
                host = sipClientProperties.serverIp
            ),
            fromHeader = SipFromHeader(
                user = sipClientProperties.user,
                host = sipClientProperties.serverIp,
                fromParamsMap = linkedMapOf(
                    "tag" to fromTag
                )
            ),
            cSeqHeader = CSeqHeader(
                cSeqNumber = 1,
                method = SipMethod.INVITE
            ),
            maxForwards = 70,
            callIdHeader = CallIdHeader(
                callId = callId
            ),
            rtpPort = sipClientProperties.rtpPort
        )
        sendSipRequest(inviteRequest.buildString(), sipClientProperties.serverIp, sipClientProperties.serverPort, channel)
        var sipInviteResponse = inviteResponseChannel.receive()
        while (sipInviteResponse.status == SipStatus.Trying || sipInviteResponse.status == SipStatus.Ringing) {
            sipInviteResponse = inviteResponseChannel.receive()
        }
        ack(inviteBranchRef!!)
        if (sipInviteResponse.status == SipStatus.Unauthorized) {
            val cnonce = UUID.randomUUID().toString()
            inviteBranchRef = "z9hG4bK${UUID.randomUUID()}"
            val newInviteRequest = SipInviteRequest(
                requestURIHeader = SipRequestURIHeader(
                    method = SipMethod.INVITE,
                    user = to,
                    host = sipClientProperties.serverIp,
                    hostParamsMap = linkedMapOf(
                        "transport" to "UDP"
                    )
                ),
                viaHeader = SipViaHeader(
                    host = sipClientProperties.clientIp,
                    port = sipClientProperties.clientPort,
                    hostParams = linkedMapOf(
                        "branch" to inviteBranchRef!!
                    )
                ),
                contactHeader = SipContactHeader(
                    user = sipClientProperties.user,
                    host = sipClientProperties.clientIp,
                    port = sipClientProperties.clientPort
                ),
                toHeader = SipToHeader(
                    user = to,
                    host = sipClientProperties.serverIp
                ),
                fromHeader = SipFromHeader(
                    user = sipClientProperties.user,
                    host = sipClientProperties.serverIp,
                    fromParamsMap = linkedMapOf(
                        "tag" to fromTag
                    )
                ),
                maxForwards = 70,
                callIdHeader = CallIdHeader(
                    callId = callId
                ),
                cSeqHeader = CSeqHeader(
                    cSeqNumber = 2,
                    method = SipMethod.INVITE
                ),
                authorizationHeader = SipAuthorizationHeader(
                    user = sipClientProperties.user,
                    realm = sipInviteResponse.wwwAuthenticateHeader!!.realm,
                    nonce = sipInviteResponse.wwwAuthenticateHeader!!.nonce,
                    serverIp = sipClientProperties.serverIp,
                    response = getResponseHash(sipClientProperties.user,
                        sipInviteResponse.wwwAuthenticateHeader!!.realm,
                        sipClientProperties.password,
                        SipMethod.INVITE,
                        sipClientProperties.serverIp,
                        sipInviteResponse.wwwAuthenticateHeader!!.nonce,
                        nc,
                        cnonce,
                        sipInviteResponse.wwwAuthenticateHeader!!.qop),
                    cnonce = cnonce,
                    nc = nc,
                    qop = sipInviteResponse.wwwAuthenticateHeader!!.qop,
                    algorithm = sipInviteResponse.wwwAuthenticateHeader!!.algorithm,
                    opaque = sipInviteResponse.wwwAuthenticateHeader!!.opaque
                ),
                rtpPort = sipClientProperties.rtpPort
            )
            sendSipRequest(newInviteRequest.buildString())
            sipInviteResponse = inviteResponseChannel.receive()
            while (sipInviteResponse.status == SipStatus.Trying || sipInviteResponse.status == SipStatus.Ringing) {
                sipInviteResponse = inviteResponseChannel.receive()
            }
            ack(inviteBranchRef!!)
        }
        if (sipInviteResponse.status == SipStatus.OK) {
            print("Invation is ok")
        } else {
            print("Invation is failed")
        }
    }

    fun stopCall() {

    }

    private fun ack(branch: String) {
        val fromTag = randomString(8)
        val ackRequest = SipAckRequest(
            requestURIHeader = SipRequestURIHeader(
                method = SipMethod.ACK,
                user = to,
                host = sipClientProperties.serverIp,
                hostParamsMap = linkedMapOf(
                    "transport" to "UDP"
                )
            ),
            viaHeader = SipViaHeader(
                host = sipClientProperties.clientIp,
                port = sipClientProperties.clientPort,
                hostParams = linkedMapOf(
                    "branch" to branch,
                    "rport" to ""
                )
            ),
            maxForwards = 70,
            toHeader = SipToHeader(
                user = sipClientProperties.user,
                host = sipClientProperties.serverIp,
                toParamsMap = linkedMapOf(
                    "tag" to branch
                )),
            fromHeader = SipFromHeader(
                user = sipClientProperties.user,
                host = sipClientProperties.serverIp,
                hostParamsMap = linkedMapOf(
                    "transport" to "UDP"
                ),
                fromParamsMap = linkedMapOf(
                    "tag" to fromTag
                )),
            callIdHeader = CallIdHeader(
                callId = callId
            ),
            cSeqHeader = CSeqHeader(
                cSeqNumber = 1,
                method = SipMethod.ACK
            )
        )
        sendSipRequest(ackRequest.buildString())
    }

}