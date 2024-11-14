package ai.nnstreamer.ml.inference.offloading.data

import ai.nnstreamer.ml.inference.offloading.domain.MobilenetClassifier
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class ImageAnalyzer(
    private val classifier: MobilenetClassifier,
    private val frameSkipRate: Int = 30,
    private val shouldAnalyze: (ImageProxy, Int) -> Boolean = { _, frameCount -> frameCount % frameSkipRate == 0 }
) : ImageAnalysis.Analyzer {
    private var frameSkipCounter = 0

    override fun analyze(imageProxy: ImageProxy) {
        if (shouldAnalyze(imageProxy, frameSkipCounter)) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val bitmap = imageProxy.toBitmap()

            classifier.classify(bitmap, rotationDegrees)
        }

        frameSkipCounter = (frameSkipCounter + 1) % frameSkipRate
        imageProxy.close()
    }
}
