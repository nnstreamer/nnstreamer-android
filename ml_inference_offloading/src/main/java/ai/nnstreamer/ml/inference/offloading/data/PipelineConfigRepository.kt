package ai.nnstreamer.ml.inference.offloading.data

import kotlinx.coroutines.flow.Flow

interface PipelineConfigsRepository {
    fun getPipelineConfigStream(uid: Int): Flow<PipelineConfig>
    suspend fun insertPipelineConfig(pipelineConfig: PipelineConfig)
    suspend fun deletePipelineConfig(pipelineConfig: PipelineConfig)
    suspend fun updatePipelineConfig(pipelineConfig: PipelineConfig)
}
