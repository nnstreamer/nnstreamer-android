package ai.nnstreamer.ml.inference.offloading.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.nnsuite.nnstreamer.Pipeline

@Entity(
    tableName = "pipelines",
    foreignKeys = [
        ForeignKey(
            entity = Model::class,
            parentColumns = ["uid"],
            childColumns = ["modelId"]
        ),
        ForeignKey(
            entity = PipelineConfig::class,
            parentColumns = ["uid"],
            childColumns = ["pipelineConfigId"]
        ),
    ]
)
data class Pipeline(
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0,
    val modelId: Int,
    val pipelineConfigId: Int,
    val port: Int,
    val status: Pipeline.State,
)
