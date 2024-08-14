package ai.nnstreamer.ml.inference.offloading.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.nnsuite.nnstreamer.Pipeline

/**
 * Represents an offloading service associated with a specific model.
 *
 * @property serviceId The unique identifier for the offloading service.
 * @property modelId The identifier for the model associated with this offloading service.
 * @property port The port number used by the offloading service.
 * @property state The state of the pipeline associated with this offloading service.
 * @property framerate The framerate of the offloading service.
 */
@Entity(
    tableName = "offloadingservices",
    foreignKeys = [
        ForeignKey(
            entity = Model::class,
            parentColumns = ["uid"],
            childColumns = ["modelId"]
        ),
    ],
    indices = [
        Index("modelId")
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
