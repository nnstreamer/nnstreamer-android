package ai.nnstreamer.ml.inference.offloading.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "models")
data class Model(
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0,
    // TODO: Fill in the information based on pre-defined config file
    val name: String,
)
