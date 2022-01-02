import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

fun main() {
    println("Hello World!")
    val reqTopic = "temps"

    runBlocking {
        val publisher = DefferedClient("tcp://127.0.0.1:1883", this)
        println("Publisher connecting...")
        publisher.connect()
        println("Publisher connected!")
        val subscribers = (0..1).map { DefferedClient("tcp://127.0.0.1:1883", this) }
        subscribers.forEach {
            it.connect()
            it.subscribe(reqTopic, 0)
        }
        println("Subscribers connected!")
        val listeners = subscribers.map {
            async {
                for(item in it.incoming) {
                    val text = item.second.payload?.let { String(it) }
                    println("message received: $text")
                    it.respond(item.second, "Got $text")
                }
            }
        }

        // publish
        val resp = publisher.request(reqTopic, "Hello, world!")
        println("Got response: $resp")

        println("Awaiting...")
        listeners.awaitAll()
    }

    println("Done!")
}
