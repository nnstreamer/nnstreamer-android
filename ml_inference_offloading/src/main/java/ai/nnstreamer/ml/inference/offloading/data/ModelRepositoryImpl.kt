package ai.nnstreamer.ml.inference.offloading.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton


/**
 * The class that implements the ModelRepository interface.
 *
 * @constructor Creates a singleton instance of the ModelRepositoryImpl class.
 * @param modelDao The ModelDao object that is used to access the database.
 */
@Singleton
class ModelRepositoryImpl @Inject constructor(private val modelDao: ModelDao) :
    ModelRepository {
    /**
     * Get the stream of all [Model]s in the database by using [modelDao].
     *
     * @return A [Flow] of a list containing the [Model]s in the database.
     * @see [ModelRepository.getAllModelsStream]
     */
    override fun getAllModelsStream(): Flow<List<Model>> = modelDao.getAllModels()

    /**
     * Get the stream of [Model]s by [uid] from the database by using [modelDao].
     *
     * @param uid the unique identifier that represents a specific [Model].
     * @return A [Flow] of the [Model]s matching the [uid].
     * @see [ModelRepository.getModelStream]
     */
    override fun getModelStream(uid: Int): Flow<Model?> = modelDao.getModel(uid)

    /**
     * Insert a [Model] into the database by using [modelDao].
     *
     * @param model the [Model] to be inserted.
     * @see [ModelRepository.insertModel]
     */
    override suspend fun insertModel(model: Model) = modelDao.insert(model)

    /**
     * Delete the given [Model] from the database by using [modelDao].
     *
     * @param model the [Model] to be deleted.
     * @see [ModelRepository.deleteModel]
     */
    override suspend fun deleteModel(model: Model) = modelDao.delete(model)

    /**
     * Update the given [Model] in the database by using [modelDao].
     *
     * @param model the [Model] to be updated.
     * @see [ModelRepository.updateModel]
     */
    override suspend fun updateModel(model: Model) = modelDao.update(model)
}
