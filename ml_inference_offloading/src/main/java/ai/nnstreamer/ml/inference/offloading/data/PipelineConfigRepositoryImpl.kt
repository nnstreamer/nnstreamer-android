package ai.nnstreamer.ml.inference.offloading.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipelineConfigRepositoryImpl @Inject constructor(private val pipelineConfigDao: PipelineConfigDao) :
    PipelineConfigsRepository {
    override fun getPipelineConfigStream(uid: Int): Flow<PipelineConfig> =
        pipelineConfigDao.getConfig(uid)

    override suspend fun insertPipelineConfig(pipelineConfig: PipelineConfig) =
        pipelineConfigDao.insert(pipelineConfig)

    override suspend fun deletePipelineConfig(pipelineConfig: PipelineConfig) =
        pipelineConfigDao.delete(pipelineConfig)

    override suspend fun updatePipelineConfig(pipelineConfig: PipelineConfig) =
        pipelineConfigDao.update(pipelineConfig)
}
