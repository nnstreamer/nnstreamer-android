package ai.nnstreamer.ml.inference.offloading.data

import android.content.Context

/**
 * Service container for Dependency injection.
 */
interface ServiceContainer {
    val modelsRepository: ModelsRepository
}

/**
 * [ServiceContainer] implementation that provides instance of [OfflineModelsRepository]
 */
class ServiceDataContainer(private val context: Context) : ServiceContainer {
    /**
     * Implementation for [ModelsRepository]
     */
    override val modelsRepository: ModelsRepository by lazy {
        OfflineModelsRepository(ModelDatabase.getDatabase(context).modelDao())
    }
}
