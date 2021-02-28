import pack.obj.RequestBuilder
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*


fun main(args: Array<String>) {
    var callId = UUID.randomUUID().toString()
    val request = RequestBuilder("10.255.250.29")
        .setMethod("REGISTER")
        .setVia(30001)
        .setMaxForward(70)
        .setContact("4091", 30001)
        .setToHeader("4091")
        .setFromHeader("4091")
        .setCallId(callId)
        .setCSeq(1, "REGISTER")
        .setExpires(20)
        //.setAuthorization("4091", "E5bTUEKL8K")
        .build()
    print(request)
    val socket = DatagramSocket(30001)
    val body = request.toByteArray()
    val datagram = DatagramPacket(body, body.size, InetAddress.getByName("10.255.250.29"), 5060)
    socket.send(datagram)
    val receivedBody = ByteArray(1024)
    socket.receive(DatagramPacket(receivedBody, receivedBody.size))
    val request2 = RequestBuilder("10.255.250.29")
        .setMethod("REGISTER")
        .setVia(30001)
        .setMaxForward(70)
        .setContact("4091", 30001)
        .setToHeader("4091")
        .setFromHeader("4091")
        .setCallId(callId)
        .setCSeq(2, "REGISTER")
        .setExpires(20)
        .setAuthorization("4091", "E5bTUEKL8K")
        .build()
    val body2 = request2.toByteArray()
    val datagram2 = DatagramPacket(body2, body2.size, InetAddress.getByName("10.255.250.29"), 5060)
    socket.send(datagram2)
    val receivedBody2 = ByteArray(1024)
    socket.receive(DatagramPacket(receivedBody2, receivedBody2.size))
    print(String(receivedBody2))
}