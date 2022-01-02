import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import java.util.concurrent.CompletableFuture

class DeferredToken(token: IMqttToken) : CompletableFuture<IMqttToken>(), MqttActionListener {
    init {
        token.actionCallback = this
    }

    override fun onSuccess(asyncActionToken: IMqttToken?) {
        complete(asyncActionToken)
    }

    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
        completeExceptionally(exception)
    }
}