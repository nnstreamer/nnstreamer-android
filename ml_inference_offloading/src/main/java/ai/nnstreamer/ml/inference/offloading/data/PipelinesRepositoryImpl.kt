package ai.nnstreamer.ml.inference.offloading.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipelinesRepositoryImpl @Inject constructor(private val pipelineDao: PipelineDao) :
    PipelinesRepository {
    override fun getPipelineStream(uid: Int): Flow<Pipeline> =
        pipelineDao.getPipeline(uid)

    override suspend fun insertPipeline(pipeline: Pipeline) =
        pipelineDao.insert(pipeline)

    override suspend fun deletePipeline(pipeline: Pipeline) =
        pipelineDao.delete(pipeline)

    override suspend fun updatePipeline(pipeline: Pipeline) =
        pipelineDao.update(pipeline)
}
