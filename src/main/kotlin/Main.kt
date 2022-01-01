import org.eclipse.paho.mqttv5.client.*
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random.Default.nextDouble


fun main(args: Array<String>) {
    println("Hello World!")

    val publisherId = UUID.randomUUID().toString()
    val publisher = MqttAsyncClient("tcp://127.0.0.1:1883", publisherId, MqttDefaultFilePersistence("/tmp"))
    val options = MqttConnectionOptions()
    options.isAutomaticReconnect = true
    options.isCleanStart = true
    options.connectionTimeout = 10

    val msgCallback = object : MqttCallback {
        override fun disconnected(disconnectResponse: MqttDisconnectResponse?) {
            println("disconnected")
        }

        override fun mqttErrorOccurred(exception: MqttException?) {
            println("mqttErrorOccurred")
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            println("messageArrived")
        }

        override fun deliveryComplete(token: IMqttToken?) {
            if(token!!.reasonCodes[0] == 16) {
                println("no one is listening")
            } else {
                println("deliveryComplete ${token!!.reasonCodes[0]}")
            }
        }

        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
            println("connectComplete")
        }

        override fun authPacketArrived(reasonCode: Int, properties: MqttProperties?) {
            println("authPacketArrived")
        }
    }

    val msgListener = object : IMqttMessageListener {
        override fun messageArrived(topic: String?, message: MqttMessage?) {
            println("messageArrived")
        }
    }
    publisher.setCallback(msgCallback)
    publisher.connect(options).waitForCompletion()
    val publishListener = object : MqttActionListener {
        override fun onSuccess(publishToken: IMqttToken) {
            println("publish succeeded")
        }

        override fun onFailure(publishToken: IMqttToken, ex: Throwable) {
            println("publish failed")
        }
    }

    // publish
    val TOPIC = "temps"
    val payload = "op1".toByteArray()
    val msg = MqttMessage(payload)
    msg.qos = 0;
    msg.properties = MqttProperties()
    msg.properties.messageExpiryInterval = 1
    msg.isRetained = false; // do not queue, stale data is bad
    val databaseId: Long = 42
    publisher.publish(TOPIC, msg, null, publishListener)

//    println("Sleeping...")
//    sleep(3000)
//    println("Woke up")

    // subscribe
    val receivedSignal = CountDownLatch(10)
    for(i in (0..2)) {
        val subscribeListener = object : MqttActionListener {
            override fun onSuccess(publishToken: IMqttToken) {
//                val text = publishToken.message?.payload?.let { String(it) }
//                println("message received: ${text}")
            }

            override fun onFailure(publishToken: IMqttToken, ex: Throwable) {
                println("receive failed")
            }
        }
        val subscriberId = UUID.randomUUID().toString()
        val subscriber = MqttAsyncClient("tcp://127.0.0.1:1883", subscriberId, MqttDefaultFilePersistence("/tmp"))
        val msgCallback = object : MqttCallback {
            override fun disconnected(disconnectResponse: MqttDisconnectResponse?) {
                println("disconnected")
            }

            override fun mqttErrorOccurred(exception: MqttException?) {
                println("mqttErrorOccurred")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                println("messageArrived $message")
            }

            override fun deliveryComplete(token: IMqttToken?) {
                if(token!!.reasonCodes[0] == 16) {
                    println("no one is listening")
                } else {
                    println("deliveryComplete")
                }
            }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
//                println("connectComplete")
            }

            override fun authPacketArrived(reasonCode: Int, properties: MqttProperties?) {
                println("authPacketArrived")
            }
        }
        subscriber.setCallback(msgCallback)
        subscriber.connect(options).waitForCompletion()
        subscriber.subscribe(TOPIC, 0, "test", subscribeListener)
    }
    println("Subscribers subscribed")

//    println("Sleeping...")
//    sleep(3000)
//    println("Woke up")

    val msg2 = MqttMessage("op2".toByteArray())
    msg2.qos = 1;
    msg2.properties = MqttProperties()
    msg2.properties.messageExpiryInterval = 1
    msg2.isRetained = false; // do not queue, stale data is bad
    publisher.publish(TOPIC, msg2, null, publishListener)

    receivedSignal.await(1, TimeUnit.MINUTES)
}