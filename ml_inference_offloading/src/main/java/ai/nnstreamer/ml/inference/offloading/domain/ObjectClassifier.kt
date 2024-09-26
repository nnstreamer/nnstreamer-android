package ai.nnstreamer.ml.inference.offloading.domain

import ai.nnstreamer.ml.inference.offloading.data.Classification
import android.graphics.Bitmap

interface ObjectClassifier {
    fun classify(bitmap: Bitmap, rotation: Int): List<Classification>
}
