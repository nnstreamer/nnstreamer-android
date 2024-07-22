package ai.nnstreamer.ml.inference.offloading.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PipelineConfigDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(pipelineConfig: PipelineConfig)

    @Update
    suspend fun update(pipelineConfig: PipelineConfig)

    @Delete
    suspend fun delete(pipelineConfig: PipelineConfig)

    @Query("SELECT * from pipelineConfigs WHERE uid = :uid")
    fun getConfig(uid: Int): Flow<PipelineConfig>
}
