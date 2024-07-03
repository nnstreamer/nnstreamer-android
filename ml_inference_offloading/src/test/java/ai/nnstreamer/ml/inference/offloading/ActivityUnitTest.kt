package ai.nnstreamer.ml.inference.offloading

import ai.nnstreamer.ml.inference.offloading.providers.ModelFileProvider
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActivityUnitTest {
    @Test
    fun testModelListLength() {
        val testModelFileProvider =
            Robolectric.buildContentProvider(ModelFileProvider::class.java).create().get()
        val activity = testModelFileProvider?.let {
            val activityController =
                Robolectric.buildActivity(MainActivity::class.java).create()

            activityController.get()
        }

        activity?.run {
            val recyclerview = this.findViewById<RecyclerView>(R.id.model_list)
            val numModelsInAssets =
                this.applicationContext.resources.assets.list("models")?.size ?: -1
            val numModels = recyclerview.adapter?.itemCount ?: 0

            assertEquals(numModelsInAssets, numModels)
        }
    }
}
