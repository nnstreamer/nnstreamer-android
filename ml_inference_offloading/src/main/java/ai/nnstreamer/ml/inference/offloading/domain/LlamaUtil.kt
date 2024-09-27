package ai.nnstreamer.ml.inference.offloading.domain

import ai.nnstreamer.ml.inference.offloading.network.findPort
import org.nnsuite.nnstreamer.NNStreamer
import org.nnsuite.nnstreamer.Pipeline
import org.nnsuite.nnstreamer.TensorsData
import org.nnsuite.nnstreamer.TensorsInfo
import java.nio.ByteBuffer

fun runLlama2(input: String, hostAddress: String, servicePort: Int, newDataCb: NewDataCb) {
    val port = findPort()
    val desc =
        "appsrc name=srcx ! application/octet-stream ! tensor_converter ! other/tensors,format=flexible ! tensor_query_client host=$hostAddress port=$port dest-host=$hostAddress dest-port=$servicePort timeout=1000000 ! tensor_sink name=sinkx"
    val pipeline = Pipeline(desc, null)

    pipeline.registerSinkCallback("sinkx", newDataCb)
    // todo: Reuse or destroy the client pipeline
    pipeline.start()

    val info = TensorsInfo()
    info.addTensorInfo(NNStreamer.TensorType.UINT8, intArrayOf(input.length, 1, 1, 1))

    val size = info.getTensorSize(0)
    val data = TensorsData.allocate(info)
    val byteBuffer: ByteBuffer = ByteBuffer.wrap(input.toByteArray())

    val buffer = TensorsData.allocateByteBuffer(size)
    buffer.put(byteBuffer)
    data.setTensorData(0, buffer)
    pipeline.inputData("srcx", data)
}
