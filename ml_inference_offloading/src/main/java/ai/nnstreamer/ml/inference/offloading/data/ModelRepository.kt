package ai.nnstreamer.ml.inference.offloading.data

import kotlinx.coroutines.flow.Flow

interface ModelRepository {
    fun getAllModelsStream(): Flow<List<Model>>
    fun getModelStream(uid: Int): Flow<Model?>
    suspend fun insertModel(model: Model)
    suspend fun deleteModel(model: Model)
    suspend fun updateModel(model: Model)
}
