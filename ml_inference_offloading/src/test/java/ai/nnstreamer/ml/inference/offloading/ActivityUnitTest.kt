package ai.nnstreamer.ml.inference.offloading


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
        val activityController = Robolectric.buildActivity(MainActivity::class.java).create()
        val activity = activityController.get()
        val recyclerview = activity.findViewById<RecyclerView>(R.id.model_list)
        val adapter = recyclerview.adapter

        adapter?.run {
            assertEquals(2, adapter.itemCount)
        }
    }
}
