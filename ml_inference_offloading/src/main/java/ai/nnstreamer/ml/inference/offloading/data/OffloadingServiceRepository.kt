package ai.nnstreamer.ml.inference.offloading.data

import kotlinx.coroutines.flow.Flow
import org.nnsuite.nnstreamer.Pipeline

/** The repository interface for accessing the [OffloadingService] data. */
interface OffloadingServiceRepository {
    /**
     * Get the stream of all [OffloadingService]s in the database.
     *
     * @return A [Flow] of a list containing the [OffloadingService]s in the database.
     */
    fun getAllOffloadingService(): Flow<List<OffloadingService>>

    /**
     * Get the stream of [OffloadingService]s by [serviceId] from the database.
     *
     * @param serviceId the unique identifier that represents a specific [OffloadingService].
     * @return a [Flow] of the [OffloadingService]s matching the [serviceId].
     */
    fun getOffloadingService(serviceId: Int): Flow<OffloadingService>

    /**
     * Insert an [OffloadingService] into the database.
     *
     * @param offloadingService the [OffloadingService] to be inserted.
     */
    suspend fun insertOffloadingService(offloadingService: OffloadingService)

    /**
     * Delete an [OffloadingService] from the offloadingservices table based on its ID.
     *
     * @param serviceId the unique identifier that represents a specific [OffloadingService] to be deleted.
     */
    suspend fun deleteOffloadingService(serviceId: Int)

    /**
     * Update the given [OffloadingService] in the database.
     *
     * @param offloadingService the [OffloadingService] to be updated.
     */
    suspend fun updateOffloadingService(offloadingService: OffloadingService)

    /**
     * Changes the state of an [OffloadingService].
     *
     * @param serviceId the unique identifier that represents a specific [OffloadingService] in the table.
     * @param state the new state to be set for the [OffloadingService].
     */
    fun changeStateOffloadingService(serviceId: Int, state: Pipeline.State)
}
