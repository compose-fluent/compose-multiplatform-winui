import kotlinx.coroutines.runBlocking
import sample.consumer.resources.Res

fun main() = runBlocking {
    check(Res.readBytes("files/message.txt").decodeToString().trim() == "Application resource")
    check(producerMessage() == "Transitive library resource")
    println("WINUI_GENERATED_AND_TRANSITIVE_RESOURCES_OK")
}
