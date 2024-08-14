package ai.nnstreamer.ml.inference.offloading.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.nnsuite.nnstreamer.Pipeline

/**
 * A Data Access Object (DAO) interface for interacting with the [OffloadingService] table.
 */
@Dao
interface OffloadingServiceDao {
    /**
     * Insert an [OffloadingService] into the offloadingservices table. If the offloadingService already exists, replace it.
     *
     * @param offloadingService the [OffloadingService] to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(offloadingService: OffloadingService)

    /**
     * Update the given [OffloadingService].
     *
     * @param offloadingService the [OffloadingService] to be updated.
     */
    @Update
    suspend fun update(offloadingService: OffloadingService)

    /**
     * Delete an [OffloadingService] from the offloadingservices table based on its ID.
     *
     * @param serviceId the unique identifier that represents a specific [OffloadingService] to delete in the table.
     */
    @Query("DELETE from offloadingservices WHERE serviceId = :serviceId")
    suspend fun delete(serviceId: Int)

    /**
     * Retrieve all [OffloadingService]s from the offloadingservices table.
     *
     * @return a [Flow] of the list consisting of all [OffloadingService]s in the table.
     */
    @Query("SELECT * from offloadingservices")
    fun getAllOffloadingService(): Flow<List<OffloadingService>>

    /**
     * Retrieve an [OffloadingService] by [serviceId] from the offloadingservices table.
     *
     * @param serviceId the unique identifier that represents a specific [OffloadingService] in the table.
     * @return a [Flow] of the matching [OffloadingService]s.
     */
    @Query("SELECT * from offloadingservices WHERE serviceId = :serviceId")
    fun getOffloadingService(serviceId: Int): Flow<OffloadingService>

    /**
     * Changes the state of an [OffloadingService] in the database based on its ID.
     *
     * @param serviceId the unique identifier that represents a specific [OffloadingService] in the table.
     * @param state the new state to be set for the [OffloadingService].
     */
    @Query("UPDATE offloadingservices SET state = :state WHERE serviceID = :serviceId")
    fun changeState(serviceId: Int, state: Pipeline.State)
}
