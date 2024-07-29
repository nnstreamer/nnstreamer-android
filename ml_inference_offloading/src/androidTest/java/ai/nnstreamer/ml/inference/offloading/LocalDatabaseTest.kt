package ai.nnstreamer.ml.inference.offloading

import ai.nnstreamer.ml.inference.offloading.data.AppDatabase
import ai.nnstreamer.ml.inference.offloading.data.Model
import ai.nnstreamer.ml.inference.offloading.data.ModelDao
import ai.nnstreamer.ml.inference.offloading.data.OffloadingService
import ai.nnstreamer.ml.inference.offloading.data.OffloadingServiceDao
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import org.nnsuite.nnstreamer.Pipeline
import java.io.IOException
import kotlin.jvm.Throws

@RunWith(AndroidJUnit4::class)
class LocalDatabaseTest {
    private lateinit var database: AppDatabase
    private lateinit var modelDao: ModelDao
    private lateinit var offloadingServiceDao: OffloadingServiceDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().context, AppDatabase::class.java
        ).build()
        modelDao = database.modelDao()
        offloadingServiceDao = database.offloadingServiceDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeOffloadingServiceAndReadInList() = runTest {
        val modelId = 1
        val model = Model(modelId, "model_name")
        modelDao.insert(model)

        val serviceId = 1
        val port = 3030
        val offloadingService = OffloadingService(serviceId, modelId, port, Pipeline.State.NULL, 0)
        offloadingServiceDao.insert(offloadingService)

        launch {
            val byId = offloadingServiceDao.getOffloadingService(serviceId)
            try {
                assertEquals(serviceId, byId.first().serviceId)
                assertEquals(modelId, byId.first().modelId)
                assertEquals(port, byId.first().port)
                assertEquals(Pipeline.State.NULL, byId.first().state)
                assertEquals(0, byId.first().framerate)
            } catch (e: NoSuchElementException) {
                assertTrue(false)
            }
        }
    }
}
