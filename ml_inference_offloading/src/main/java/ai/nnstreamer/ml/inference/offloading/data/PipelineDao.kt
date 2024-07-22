package ai.nnstreamer.ml.inference.offloading.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PipelineDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(pipeline: Pipeline)

    @Update
    suspend fun update(pipeline: Pipeline)

    @Delete
    suspend fun delete(pipeline: Pipeline)

    @Query("SELECT * from pipelines WHERE uid = :uid")
    fun getPipeline(uid: Int): Flow<Pipeline>
}
