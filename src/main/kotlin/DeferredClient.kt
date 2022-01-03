import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.eclipse.paho.mqttv5.client.*
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException

class DeferredClient(
    url: String,
    val scope: CoroutineScope
) : MqttCallback {
    val publisherId = UUID.randomUUID().toString()
    val client = MqttAsyncClient(url, publisherId, MqttDefaultFilePersistence("/tmp"))
    val fromMqtt = Channel<Pair<String, MqttMessage>>()
    val incoming = Channel<Pair<String, MqttMessage>>()
    val requests = ConcurrentHashMap<String, CompletableFuture<String>>()
    val responseTopic = "responses"

    init {
        client.setCallback(this)
    }

    fun connect() {
        val options = MqttConnectionOptions()
        options.isAutomaticReconnect = true
        options.isCleanStart = true
        options.connectionTimeout = 10
        client.connect(options).waitForCompletion()
        subscribe(responseTopic, 0)
        scope.launch {
            for(msg in fromMqtt) {
                if(msg.first == responseTopic) {
                    val requestId = String(msg.second.properties.correlationData)
                    val payload = String(msg.second.payload)
//                    println("Got response to $requestId: $payload")
                    requests.remove(requestId)?.complete(payload)
                } else {
                    incoming.send(msg)
                }
            }
        }
    }

    fun publish(topic: String, message: MqttMessage, context: Any?): DeferredAction {
        val listener = DeferredAction()
        client.publish(topic, message, context, listener)
        return listener
    }

    fun respond(request: MqttMessage, response: String) {
        val msg = MqttMessage(response.toByteArray())
        msg.qos = 0;
        msg.properties = MqttProperties()
        msg.properties.messageExpiryInterval = 1
        msg.isRetained = false; // do not queue, stale data is bad
        msg.properties.correlationData = request.properties.correlationData
        client.publish(responseTopic, msg)
    }

    suspend fun request(topic: String, payload: String): String? {
        val uuid = UUID.randomUUID().toString()
        val msg = MqttMessage(payload.toByteArray())
        msg.qos = 1;
        msg.properties = MqttProperties()
        msg.properties.messageExpiryInterval = 1
        msg.isRetained = false; // do not queue, stale data is bad
        msg.properties.responseTopic = responseTopic
        msg.properties.correlationData = uuid.toByteArray()

        val cofu = CompletableFuture<String>()
        requests[uuid] = cofu
        val token = DeferredToken(client.publish(topic, msg)).await()
        println("Token=$token")
        scope.launch {
            delay(3000)
            requests.remove(uuid)?.completeExceptionally(TimeoutException("Request ${uuid} did not complete"))
        }
        return cofu.await()
    }

    fun subscribe(topic: String, qos: Int) {
        client.subscribe(topic, qos).waitForCompletion()
    }

    override fun disconnected(disconnectResponse: MqttDisconnectResponse?) {
        TODO("Not yet implemented")
    }

    override fun mqttErrorOccurred(exception: MqttException?) {
        TODO("Not yet implemented")
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {
        scope.launch {
//            println("DefferedClient got message")
            fromMqtt.send(Pair(topic!!, message!!))
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