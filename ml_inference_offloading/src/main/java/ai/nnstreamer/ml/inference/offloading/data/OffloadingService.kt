package ai.nnstreamer.ml.inference.offloading.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.nnsuite.nnstreamer.Pipeline

@Entity(
    tableName = "offloadingservices",
    foreignKeys = [
        ForeignKey(
            entity = Model::class,
            parentColumns = ["uid"],
            childColumns = ["modelId"]
        ),
    ]
)
data class OffloadingService(
    @PrimaryKey(autoGenerate = true)
    val serviceId: Int = 0,
    val modelId: Int,
    val port: Int,
    val state: Pipeline.State,
    val framerate: Int,
)
