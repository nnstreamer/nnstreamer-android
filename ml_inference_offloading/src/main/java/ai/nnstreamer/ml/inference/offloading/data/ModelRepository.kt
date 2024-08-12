package ai.nnstreamer.ml.inference.offloading.data

import kotlinx.coroutines.flow.Flow

/** The repository interface for accessing the model data. */
interface ModelRepository {
    /**
     * Get the stream of all [Model]s in the database.
     *
     * @return A [Flow] of a list containing the [Model]s in the database.
     */
    fun getAllModelsStream(): Flow<List<Model>>

    /**
     * Get the stream of [Model]s by [uid] from the database.
     *
     * @param uid the unique identifier that represents a specific [Model]
     * @return A [Flow] of the [Model]s matching the [uid].
     */
    fun getModelStream(uid: Int): Flow<Model?>

    /**
     * Insert a [Model] into the database.
     *
     * @param model the [Model] to be inserted.
     */
    suspend fun insertModel(model: Model)

    /**
     * Delete the given [Model] from the database.
     *
     * @param model the [Model] to be deleted.
     */
    suspend fun deleteModel(model: Model)

    /**
     * Update the given [Model] in the database.
     *
     * @param model the [Model] to be updated.
     */
    suspend fun updateModel(model: Model)
}
