package ai.nnstreamer.ml.inference.offloading.data

import ai.nnstreamer.ml.inference.offloading.domain.MobilenetClassifier
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class ImageAnalyzer(
    private val classifier: MobilenetClassifier,
) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val bitmap = imageProxy.toBitmap()

        classifier.classify(bitmap, rotationDegrees)

        imageProxy.close()
    }
}
