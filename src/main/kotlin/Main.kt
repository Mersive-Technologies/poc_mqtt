import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties

fun main() {
    println("Hello World!")
    val TOPIC = "temps"

    runBlocking {
        val publisher = DefferedClient("tcp://127.0.0.1:1883", this)
        println("Publisher connecting...")
        publisher.connect().await()
        println("Publisher connected!")
        val subscribers = (0..1).map { DefferedClient("tcp://127.0.0.1:1883", this) }
        subscribers.map {
            async {
                it.connect().await()
                it.subscribe(TOPIC, 0).await()
            }
        }.awaitAll()
        println("Subscribers connected!")
        val listeners = subscribers.map {
            async {
                for(item in it.incoming) {
                    val text = item.second?.payload?.let { String(it) }
                    println("message received: ${text}")
                }
            }
        }

        // publish
        val payload = "op1".toByteArray()
        val msg = MqttMessage(payload)
        msg.qos = 0;
        msg.properties = MqttProperties()
        msg.properties.messageExpiryInterval = 1
        msg.isRetained = false; // do not queue, stale data is bad
        publisher.publish(TOPIC, msg, null).await()

        println("Awaiting...")
        listeners.awaitAll()
    }

    println("Done!")
}
