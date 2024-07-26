package ai.nnstreamer.ml.inference.offloading.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.nnsuite.nnstreamer.Pipeline

@Dao
interface OffloadingServiceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(offloadingService: OffloadingService)

    @Update
    suspend fun update(offloadingService: OffloadingService)

    @Query("DELETE from offloadingservices WHERE serviceId = :serviceId")
    suspend fun delete(serviceId: Int)

    @Query("SELECT * from offloadingservices")
    fun getAllOffloadingService(): Flow<List<OffloadingService>>

    @Query("SELECT * from offloadingservices WHERE serviceId = :serviceId")
    fun getOffloadingService(serviceId: Int): Flow<OffloadingService>

    @Query("UPDATE offloadingservices SET state = :state WHERE serviceID = :serviceId")
    fun changeState(serviceId: Int, state: Pipeline.State)
}
