package ai.nnstreamer.ml.inference.offloading.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pipelineConfigs")
data class PipelineConfig(
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0,
    val srcCaps: String,
    val sinkCaps: String,
    val properties: String,
)
