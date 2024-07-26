package ai.nnstreamer.ml.inference.offloading.data

import kotlinx.coroutines.flow.Flow
import org.nnsuite.nnstreamer.Pipeline

interface OffloadingServiceRepository {
    fun getAllOffloadingService(): Flow<List<OffloadingService>>
    fun getOffloadingService(serviceId: Int): Flow<OffloadingService>
    suspend fun insertOffloadingService(offloadingService: OffloadingService)
    suspend fun deleteOffloadingService(serviceId: Int)
    suspend fun updateOffloadingService(offloadingService: OffloadingService)
    fun changeStateOffloadingService(serviceId: Int, state: Pipeline.State)
}
