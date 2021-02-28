import kotlinx.coroutines.runBlocking
import ru.stech.ClientExecutor


fun main(args: Array<String>) {
    val ce = ClientExecutor()
    runBlocking {
        ce.startClient()
        ce.start()
    }
    print("stopping!!!")
}
