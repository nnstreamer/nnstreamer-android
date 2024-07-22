package ai.nnstreamer.ml.inference.offloading

import ai.nnstreamer.ml.inference.offloading.data.AppDatabase
import ai.nnstreamer.ml.inference.offloading.data.Model
import ai.nnstreamer.ml.inference.offloading.data.ModelDao
import ai.nnstreamer.ml.inference.offloading.data.PipelineConfig
import ai.nnstreamer.ml.inference.offloading.data.PipelineConfigDao
import ai.nnstreamer.ml.inference.offloading.data.PipelineDao
import ai.nnstreamer.ml.inference.offloading.di.DatabaseModule
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import kotlin.jvm.Throws

@RunWith(AndroidJUnit4::class)
class LocalDatabaseTest {
    private lateinit var database: AppDatabase
    private var databaseModule = DatabaseModule()

    private lateinit var modelDao: ModelDao
    private lateinit var pipelineDao: PipelineDao
    private lateinit var pipelineConfigDao: PipelineConfigDao

    @Before
    fun setDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = databaseModule.provideAppDatabase(context)
        modelDao = database.modelDao()
        pipelineDao = database.pipelineDao()
        pipelineConfigDao = database.pipelineConfigDao()
    }

    @Test
    @Throws(Exception::class)
    fun writeAndRead() = runTest {
        val srcCaps =
            "other/tensors,num_tensors=1,format=static,dimensions=(string)3:224:224:1,types=uint8,framerate=0/1"
        val sinkCaps =
            "other/tensors,num_tensors=1,format=static,dimensions=(string)1001:1,types=uint8,framerate=0/1"
        val properties = "framework=tensorflow-lite"
        val pipelineConfig = PipelineConfig(
            1,
            srcCaps,
            sinkCaps,
            properties
        )

        pipelineConfigDao.insert(pipelineConfig)

        launch {
            val repeatableFlow = pipelineConfigDao.getConfig(1).take(1)
            repeatableFlow.collect {
                assertEquals(srcCaps, it.srcCaps)
                assertEquals(sinkCaps, it.sinkCaps)
                assertEquals(properties, it.properties)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun foreignKey() = runTest {
        val modelName = "test"
        val model = Model(
            1,
            modelName
        )
        modelDao.insert(model)

        val srcCaps =
            "other/tensors,num_tensors=1,format=static,dimensions=(string)3:224:224:1,types=uint8,framerate=0/1"
        val sinkCaps =
            "other/tensors,num_tensors=1,format=static,dimensions=(string)1001:1,types=uint8,framerate=0/1"
        val properties = "framework=tensorflow-lite"
        val pipelineConfig = PipelineConfig(
            1,
            srcCaps,
            sinkCaps,
            properties
        )
        pipelineConfigDao.insert(pipelineConfig)

        val pipeline = ai.nnstreamer.ml.inference.offloading.data.Pipeline(
            1,
            1,
            1,
            3030,
            org.nnsuite.nnstreamer.Pipeline.State.NULL
        )
        pipelineDao.insert(pipeline)

        launch {
            val repeatableFlow = pipelineDao.getPipeline(1).take(1)
            repeatableFlow.collect {
                assertEquals(1, it.uid)
                assertEquals(1, it.modelId)
                assertEquals(1, it.pipelineConfigId)
            }
        }
    }
}
