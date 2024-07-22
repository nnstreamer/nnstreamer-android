package ai.nnstreamer.ml.inference.offloading.data

import kotlinx.coroutines.flow.Flow

interface PipelinesRepository {
    fun getPipelineStream(uid: Int): Flow<Pipeline>
    suspend fun insertPipeline(pipeline: Pipeline)
    suspend fun deletePipeline(pipeline: Pipeline)
    suspend fun updatePipeline(pipeline: Pipeline)
}
