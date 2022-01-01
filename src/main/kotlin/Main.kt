import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random.Default.nextDouble


fun main(args: Array<String>) {
    println("Hello World!")

    val publisherId = UUID.randomUUID().toString()
    val publisher = MqttAsyncClient("tcp://127.0.0.1:1883", publisherId, MqttDefaultFilePersistence("/tmp"))
    val options = MqttConnectOptions()
    options.isAutomaticReconnect = true
    options.isCleanSession = true
    options.connectionTimeout = 10

    val msgCallback: MqttCallback = object : MqttCallback {
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

    val mPublishCallback = object : IMqttActionListener {
        override fun onSuccess(publishToken: IMqttToken) {
            println("onSuccess")
        }

        override fun onFailure(publishToken: IMqttToken, ex: Throwable) {
            println("onFailure")
        }
    }

    val TOPIC = "temps"

    // subscribe
    val receivedSignal = CountDownLatch(10)
    for(i in (0..1)) {
        val subscriberId = UUID.randomUUID().toString()
        val subscriber = MqttAsyncClient("tcp://127.0.0.1:1883", subscriberId, MqttDefaultFilePersistence("/tmp"))
        subscriber.connect(options).waitForCompletion()
        subscriber.subscribe(TOPIC, 2) { topic, msg ->
            val payload: ByteArray = msg.getPayload()
            println( "[I82] Message received: topic=${topic}, payload=${String(payload)}")
            receivedSignal.countDown()
        }
    }

    // publish
    publisher.setCallback(msgCallback)
    publisher.connect(options).waitForCompletion()
    val temp: Double = 80 + nextDouble() * 20.0
    val payload = String.format("T:%04.2f", temp).toByteArray()
    val msg = MqttMessage(payload)
    msg.setQos(2);
    msg.setRetained(false); // do not queue, stale data is bad
    val databaseId: Long = 42
    publisher.publish(TOPIC, msg, null, mPublishCallback)

    receivedSignal.await(1, TimeUnit.MINUTES)
}