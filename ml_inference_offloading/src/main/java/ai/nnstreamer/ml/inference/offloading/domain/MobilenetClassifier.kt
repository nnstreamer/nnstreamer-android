package ai.nnstreamer.ml.inference.offloading.domain

import ai.nnstreamer.ml.inference.offloading.MessageType
import ai.nnstreamer.ml.inference.offloading.data.Classification
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.nnsuite.nnstreamer.NNStreamer
import org.nnsuite.nnstreamer.Pipeline
import org.nnsuite.nnstreamer.TensorsData
import org.nnsuite.nnstreamer.TensorsInfo
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.experimental.and

object MobileNetPipeline {
    private const val TAG = "MobileNetPipeline"
    private const val SRC_PAD = "srcx"
    private const val SINK_PAD = "sinkx"
    var onResultsCb: ((List<Classification>) -> Unit)? = null
    var pipeline: Pipeline? = null
    var labels = arrayOf<String>()

    private fun stateChangeCallback(): Pipeline.StateChangeCallback =
        Pipeline.StateChangeCallback { state ->
            when (state) {
                Pipeline.State.UNKNOWN -> {
                    Log.d(TAG, state.toString())
                }

                Pipeline.State.NULL -> {
                    Log.d(TAG, state.toString())
                }

                Pipeline.State.READY -> {
                    Log.d(TAG, state.toString())
                }

                Pipeline.State.PAUSED -> {
                    Log.d(TAG, state.toString())
                }

                Pipeline.State.PLAYING -> {
                    Log.d(TAG, state.toString())
                }

                null -> {
                    Log.e(TAG, "null")
                }
            }
        }


    fun incomingNewDataCb(): Pipeline.NewDataCallback = Pipeline.NewDataCallback { data ->
        var label = ""
        var maxScore = 0
        // todo: Use a list/array compression
        data.getTensorData(0).run {
            var index = -1

            for (i in 0..1000) {
                val score: Int = (this.get(i).and(0xFF.toByte())).toInt()

                if (score > maxScore) {
                    maxScore = score
                    index = i
                }
            }

            label = labels[index]
        }

        onResultsCb?.run {
            val classifications = mutableListOf<Classification>()

            classifications.add(Classification(label, maxScore.toDouble()))
            this(classifications)
        }
    }

    fun create(filter: String) {
        val desc =
            "appsrc caps=image/jpeg name=$SRC_PAD ! jpegdec ! videoconvert ! videoscale ! tensor_converter ! $filter ! tensor_sink name=$SINK_PAD"
        pipeline = Pipeline(desc, stateChangeCallback()).apply {
            registerSinkCallback(
                SINK_PAD,
                incomingNewDataCb()
            )
            start()
        }
    }

    var state = pipeline?.getState()
    val pushInputData: (inData: TensorsData) -> Unit = { inData ->
        pipeline?.inputData(SRC_PAD, inData)
    }
}

class MobilenetClassifier(
    messenger: Messenger?,
    onResults: (List<Classification>) -> Unit
) : ObjectClassifier {
    init {
        MobileNetPipeline.onResultsCb = onResults
        if (MobileNetPipeline.pipeline == null) {
            val msg = Message.obtain(null, MessageType.REQ_OBJ_CLASSIFICATION_FILTER.value)
            val callback = Handler.Callback {
                CoroutineScope(Dispatchers.Main.immediate).launch {
                    supervisorScope {
                        val filter = async {
                            it.data.getString("filter") ?: ""
                        }.await()
                        MobileNetPipeline.create(filter)
                        val labels = async { it.data.getStringArray("labels") }.await()
                        if (labels != null) {
                            MobileNetPipeline.labels = labels
                        }
                    } // supervisorScope
                } // CoroutineScope
                true
            } // Handler.Callback
            msg.replyTo = Messenger(Handler(Looper.getMainLooper(), callback))
            messenger?.send(msg)
        }
    }

    override fun classify(bitmap: Bitmap, rotation: Int): List<Classification> {
        val outputStream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val info = TensorsInfo().apply {
            addTensorInfo(NNStreamer.TensorType.UINT8, intArrayOf(outputStream.size(), 1))
        }
        val data = TensorsData.allocate(info)
        val byteBuffer = ByteBuffer.wrap(outputStream.toByteArray())
        val buffer = TensorsData.allocateByteBuffer(info.getTensorSize(0))

        buffer.put(byteBuffer)
        data.setTensorData(0, buffer)
        MobileNetPipeline.pushInputData(data)

        return listOf<Classification>()
    }
}
