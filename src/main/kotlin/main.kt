import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.stech.ClientFactory

fun main(args: Array<String>) {
    val factory = ClientFactory(3)
    val client = factory.newClient(
        user = "4093",
        password = "E5bTUEKL8K",
        clientPort = 30001,
        serverPort = 5060,
        serverIp = "10.255.250.29"
    )
    client.startListening()
    GlobalScope.launch {
        client.register()
        client.startCall()
        //ce.unregister()
    }
    runBlocking {
        while (true) {}
    }
    print("stopping!!!")
}
