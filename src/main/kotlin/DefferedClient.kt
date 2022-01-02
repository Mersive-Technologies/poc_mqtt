import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.eclipse.paho.mqttv5.client.*
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import java.util.*

class DefferedClient(
    url: String,
    val scope: CoroutineScope
) : MqttCallback {
    val publisherId = UUID.randomUUID().toString()
    val client = MqttAsyncClient(url, publisherId, MqttDefaultFilePersistence("/tmp"))
    val incoming = Channel<Pair<String, MqttMessage>>()

    init {
        client.setCallback(this)
    }

    suspend fun connect(): DeferredToken {
        val options = MqttConnectionOptions()
        options.isAutomaticReconnect = true
        options.isCleanStart = true
        options.connectionTimeout = 10
        return DeferredToken(client.connect(options))
    }

    fun publish(topic: String, message: MqttMessage, context: Any?): DeferredAction {
        val listener = DeferredAction()
        client.publish(topic, message, context, listener)
        return listener
    }

    fun subscribe(topic: String, qos: Int): DeferredToken {
        return DeferredToken(client.subscribe(topic, qos))
    }

    override fun disconnected(disconnectResponse: MqttDisconnectResponse?) {
        TODO("Not yet implemented")
    }

    override fun mqttErrorOccurred(exception: MqttException?) {
        TODO("Not yet implemented")
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {
        scope.launch {
            incoming.send(Pair(topic!!, message!!))
        }
    }

    override fun deliveryComplete(token: IMqttToken?) {
        // no op
    }

    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
        // no op
    }

    override fun authPacketArrived(reasonCode: Int, properties: MqttProperties?) {
        // no op
    }
}