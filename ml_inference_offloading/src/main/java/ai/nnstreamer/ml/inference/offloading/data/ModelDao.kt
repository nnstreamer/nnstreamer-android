package ai.nnstreamer.ml.inference.offloading.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(model: Model)

    @Update
    suspend fun update(model: Model)

    @Delete
    suspend fun delete(model: Model)

    @Query("SELECT * from models WHERE uid = :uid")
    fun getModel(uid: Int): Flow<Model>

    @Query("SELECT * from models ORDER BY name ASC")
    fun getAllModels(): Flow<List<Model>>
}
