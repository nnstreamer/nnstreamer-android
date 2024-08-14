package ai.nnstreamer.ml.inference.offloading.data

import kotlinx.coroutines.flow.Flow
import org.nnsuite.nnstreamer.Pipeline
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The class that implements the OffloadingServiceRepository interface.
 *
 * @constructor Creates a singleton instance of the OffloadingServiceRepositoryImpl class.
 * @param offloadingServiceDao The [OffloadingServiceDao] object that is used to access the database.
 */
@Singleton
class OffloadingServiceRepositoryImpl @Inject constructor(private val offloadingServiceDao: OffloadingServiceDao) :
    OffloadingServiceRepository {
    /**
     * Get the stream of all [OffloadingService]s in the database by using offloadingServiceDao.
     *
     * @return a [Flow] of a list containing the [OffloadingService]s in the database.
     * @see [OffloadingServiceRepository.getAllOffloadingService]
     */
    override fun getAllOffloadingService(): Flow<List<OffloadingService>> =
        offloadingServiceDao.getAllOffloadingService()

    /**
     * Get the stream of [OffloadingService]s by [serviceId] from the database by using offloadingServiceDao.
     *
     * @param serviceId the unique identifier that represents a specific [OffloadingService].
     * @return a [Flow] of the [OffloadingService]s matching the [serviceId].
     * @see [OffloadingServiceRepository.getOffloadingService]
     */
    override fun getOffloadingService(serviceId: Int): Flow<OffloadingService> =
        offloadingServiceDao.getOffloadingService(serviceId)

    /**
     * Insert a [OffloadingService] into the database by using offloadingServiceDao.
     *
     * @param offloadingService the [OffloadingService] to be inserted.
     * @see [OffloadingServiceRepository.insertOffloadingService]
     */
    override suspend fun insertOffloadingService(offloadingService: OffloadingService) =
        offloadingServiceDao.insert(offloadingService)

    /**
     * Delete an [OffloadingService] from the offloadingservices table based on its ID by using offloadingServiceDao.
     *
     * @param serviceId the unique identifier that represents a specific [OffloadingService] to be deleted.
     * @see [OffloadingServiceRepository.deleteOffloadingService]
     */
    override suspend fun deleteOffloadingService(serviceId: Int) =
        offloadingServiceDao.delete(serviceId)

    /**
     * Update the given [OffloadingService] in the database by using offloadingServiceDao.
     *
     * @param offloadingService the [OffloadingService] to be updated.
     * @see [OffloadingServiceRepository.updateOffloadingService]
     */
    override suspend fun updateOffloadingService(offloadingService: OffloadingService) =
        offloadingServiceDao.update(offloadingService)

    /**
     * Changes the state of an [OffloadingService] by using offloadingServiceDao.
     *
     * @param serviceId the unique identifier that represents a specific [OffloadingService] in the table.
     * @param state the new state to be set for the [OffloadingService].
     * @see [OffloadingServiceRepository.changeStateOffloadingService]
     */
    override fun changeStateOffloadingService(serviceId: Int, state: Pipeline.State) =
        offloadingServiceDao.changeState(serviceId, state)
}
