package ai.nnstreamer.ml.inference.offloading.domain

import android.os.Message
import android.os.Messenger
import org.nnsuite.nnstreamer.Pipeline
import org.nnsuite.nnstreamer.TensorsData

class NewDataCb(private val messenger: Messenger?) : Pipeline.NewDataCallback {
    override fun onNewDataReceived(data: TensorsData?) {
        val received = data?.getTensorData(0)
        received?.let {
            val result = mutableListOf<Byte>()

            for (byte in received.array()) {
                if (byte != 0.toByte()) {
                    result.add(byte)
                }
            }

            val response = Message.obtain()
            response.data.putString("response", String(result.toByteArray(), Charsets.UTF_8))
            messenger?.send(response)
        }
    }
}
