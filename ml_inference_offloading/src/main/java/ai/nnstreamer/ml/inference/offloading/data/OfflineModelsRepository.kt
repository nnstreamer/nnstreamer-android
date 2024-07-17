package ai.nnstreamer.ml.inference.offloading.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class OfflineModelsRepository @Inject constructor(private val modelDao: ModelDao) :
    ModelsRepository {
    override fun getAllModelsStream(): Flow<List<Model>> = modelDao.getAllModels()
    override fun getModelStream(uid: Int): Flow<Model?> = modelDao.getModel(uid)
    override suspend fun insertModel(model: Model) = modelDao.insert(model)
    override suspend fun deleteModel(model: Model) = modelDao.delete(model)
    override suspend fun updateModel(model: Model) = modelDao.update(model)
}
