import org.eclipse.paho.client.mqttv3.IMqttClient
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random.Default.nextDouble


fun main(args: Array<String>) {
    println("Hello World!")

    val publisherId = UUID.randomUUID().toString()
    val publisher = MqttClient("tcp://127.0.0.1:1883", publisherId, MqttDefaultFilePersistence("/tmp"))

    val options = MqttConnectOptions()
    options.isAutomaticReconnect = true
    options.isCleanSession = true
    options.connectionTimeout = 10
    publisher.connect(options)

    val TOPIC = "temps"

    val subscriberId = UUID.randomUUID().toString()
    val subscriber = MqttClient("tcp://127.0.0.1:1883", subscriberId, MqttDefaultFilePersistence("/tmp"))
    subscriber.connect(options)
    val receivedSignal = CountDownLatch(10)
    subscriber.subscribe(TOPIC) { topic, msg ->
        val payload: ByteArray = msg.getPayload()
        println( "[I82] Message received: topic=${topic}, payload=${String(payload)}")
        receivedSignal.countDown()
    }

    val temp: Double = 80 + nextDouble() * 20.0
    val payload = String.format("T:%04.2f", temp).toByteArray()
    val msg = MqttMessage(payload)
    msg.setQos(0);
    msg.setRetained(false); // do not queue, stale data is bad
    publisher.publish(TOPIC, msg);

    receivedSignal.await(1, TimeUnit.MINUTES)
}