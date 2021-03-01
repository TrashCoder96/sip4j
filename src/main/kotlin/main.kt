import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import ru.stech.Client

fun main(args: Array<String>) {
    val dispatcher = newFixedThreadPoolContext(3, "co")
    val ce = Client(
        user = "4091",
        password = "E5bTUEKL8K",
        clientPort = 30001,
        serverPort = 5060,
        serverIp = "10.255.250.29",
        dispatcher = dispatcher
    )
    ce.startListening()
    ce.register()
    runBlocking {
        while (true) {}
    }
    print("stopping!!!")
}
