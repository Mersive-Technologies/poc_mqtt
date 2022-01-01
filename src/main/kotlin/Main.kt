import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random.Default.nextDouble


fun main(args: Array<String>) {
    println("Hello World!")

    val publisherId = UUID.randomUUID().toString()
    val publisher = MqttClient("tcp://127.0.0.1:1883", publisherId, MqttDefaultFilePersistence("/tmp"))

    val mPublishCallback: MqttCallback = object : MqttCallback {
        override fun connectionLost(cause: Throwable?) {
            println("connectionLost")
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            println("messageArrived")
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
            println("deliveryComplete")
        }
    }

    val options = MqttConnectOptions()
    options.isAutomaticReconnect = true
    options.isCleanSession = true
    options.connectionTimeout = 10
    publisher.setCallback(mPublishCallback)
    publisher.timeToWait = 1000
    publisher.connect(options)

    val TOPIC = "temps"

    // subscribe
    val receivedSignal = CountDownLatch(10)
    for(i in (0..1)) {
        val subscriberId = UUID.randomUUID().toString()
        val subscriber = MqttClient("tcp://127.0.0.1:1883", subscriberId, MqttDefaultFilePersistence("/tmp"))
        subscriber.connect(options)
        subscriber.subscribe(TOPIC) { topic, msg ->
            val payload: ByteArray = msg.getPayload()
            println( "[I82] Message received: topic=${topic}, payload=${String(payload)}")
            receivedSignal.countDown()
        }
    }

    // publish
    val temp: Double = 80 + nextDouble() * 20.0
    val payload = String.format("T:%04.2f", temp).toByteArray()
    val msg = MqttMessage(payload)
    msg.setQos(1);
    msg.setRetained(false); // do not queue, stale data is bad
    val databaseId: Long = 42
    publisher.publish(TOPIC, msg);

    receivedSignal.await(1, TimeUnit.MINUTES)
}