package ai.nnstreamer.ml.inference.offloading.data

import kotlinx.coroutines.flow.Flow
import org.nnsuite.nnstreamer.Pipeline
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OffloadingServiceRepositoryImpl @Inject constructor(private val offloadingServiceDao: OffloadingServiceDao) :
    OffloadingServiceRepository {
    override fun getAllOffloadingService(): Flow<List<OffloadingService>> =
        offloadingServiceDao.getAllOffloadingService()

    override fun getOffloadingService(serviceId: Int): Flow<OffloadingService> =
        offloadingServiceDao.getOffloadingService(serviceId)

    override suspend fun insertOffloadingService(offloadingService: OffloadingService) =
        offloadingServiceDao.insert(offloadingService)

    override suspend fun deleteOffloadingService(serviceId: Int) =
        offloadingServiceDao.delete(serviceId)

    override suspend fun updateOffloadingService(offloadingService: OffloadingService) =
        offloadingServiceDao.update(offloadingService)

    override fun changeStateOffloadingService(serviceId: Int, state: Pipeline.State) =
        offloadingServiceDao.changeState(serviceId, state)
}
