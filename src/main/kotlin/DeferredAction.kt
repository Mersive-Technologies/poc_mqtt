import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import java.util.concurrent.CompletableFuture

class DeferredAction() : CompletableFuture<IMqttToken?>(), MqttActionListener {
    override fun onSuccess(asyncActionToken: IMqttToken?) {
        this.complete(asyncActionToken)
    }

    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
        this.completeExceptionally(exception)
    }
}