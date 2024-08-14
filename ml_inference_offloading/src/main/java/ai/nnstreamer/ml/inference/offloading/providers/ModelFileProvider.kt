package ai.nnstreamer.ml.inference.offloading.providers

import ai.nnstreamer.ml.inference.offloading.App
import ai.nnstreamer.ml.inference.offloading.R
import ai.nnstreamer.ml.inference.offloading.data.Model
import ai.nnstreamer.ml.inference.offloading.data.ModelRepositoryImpl
import ai.nnstreamer.ml.inference.offloading.data.PreferencesDataStore
import ai.nnstreamer.ml.inference.offloading.data.PreferencesDataStoreImpl
import android.content.Context
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import javax.inject.Inject

/**
 * A FileProvider for the models.
 *
 * This component is responsible for preloading the bundled models to the App's private external
 * storage and generating a URI for the specified model.
 *
 * @constructor Creates a ModelFileProvider.
 * @property modelsRepository the class that implements the repository pattern for the models.
 * @property preferencesDataStore the class that implements the [PreferencesDataStore] interface.
 * @see [ModelRepositoryImpl]
 * @see [PreferencesDataStoreImpl]
 */
class ModelFileProvider : FileProvider(R.xml.file_paths) {
    private val logTag = "FileProvider"

    @Inject
    lateinit var modelsRepository: ModelRepositoryImpl

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStoreImpl

    private lateinit var copyAssetsToExternalJob: Job

    companion object {
        val assetPaths = listOf("models")
    }

    /**
     * Parse a given model configuration file
     *
     * @param confAbsPath the absolute path to the model configuration file to parse.
     * @return a map of the names to the JSON objects of the model configuration file.
     */
    private fun parseModelConf(confAbsPath: String): Map<String, JSONObject> {
        val confFile = File(confAbsPath)
        val jsonString = runCatching {
            FileReader(confFile).buffered().use {
                it.readText()
            }
        }.getOrNull()
        val ret = mutableMapOf<String, JSONObject>()
        // A configuration file of the "Single" type
        val keys = Pair("single", "information")

        jsonString?.run {
            val jsonObject = JSONTokener(this).nextValue() as JSONObject

            runCatching {
                keys.toList().forEach { key ->
                    if (jsonObject.has(key)) {
                        ret[key] = jsonObject.getJSONObject(key)
                    }
                }
            }.getOrNull()
        }

        return ret
    }

    /**
     * Copies the assets to the external storage.
     *
     * @param ctx the context of the application.
     * @param path the path to the specified external storage to copy the assets.
     */
    private fun copyAssetsToExternal(ctx: Context, path: String) {
        val resAssets = ctx.resources.assets
        val externalDir = ctx.getExternalFilesDir(null)?.resolve(path)
            ?: throw IOException("Failed to resolve $path in the App's private external storage")

        try {
            if (!externalDir.isDirectory && !externalDir.mkdir()) {
                throw IOException("Failed to create $externalDir")
            }
        } catch (e: SecurityException) {
            val msg = e.message ?: "none"

            throw IOException("Failed to access $externalDir for the security reason (details: $msg)")
        }

        resAssets.list(path)?.filterNotNull()?.filter {
            // Copy the bundled asset files if they do not exist in the private external storage
            !externalDir.resolve(it).exists()
        }?.onEach { name ->
            resAssets.open("$path/$name").use { stream ->
                stream.copyTo(FileOutputStream(externalDir.resolve(name)))
                // TODO: Place the following code in the other proper location
                if (name.endsWith("conf")) {
                    val map = parseModelConf(externalDir.resolve(name).toString())
                    val modelId = runBlocking {
                        preferencesDataStore.getIncrementalCounter()
                    }
                    val model = Model(
                        modelId,
                        name.removeSuffix(".conf"),
                        map.getOrDefault("single", JSONObject()),
                        map.getOrDefault("information", JSONObject()),
                    )

                    // TODO: Need to check the Model entity's integrity
                    CoroutineScope(Dispatchers.IO).launch {
                        modelsRepository.insertModel(model)
                    }
                }
            }
        }
    }

    /**
     * A lifecycle callback method that overrides [FileProvider.onCreate].
     *
     * This callback is invoked when the provider is being created.
     *
     * @return True if the provider was successfully created, false otherwise.
     */
    @Override
    override fun onCreate(): Boolean {
        // Dependency Injection to AppComponent
        App.instance.appComponent.inject(this)

        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            Log.e(logTag, "CoroutineExceptionHandler got $exception")
        }

        copyAssetsToExternalJob =
            CoroutineScope(Dispatchers.IO).launch(coroutineExceptionHandler) {
                context?.run {
                    assetPaths.onEach { path ->
                        try {
                            copyAssetsToExternal(this, path)
                        } catch (e: Exception) {
                            Log.e(logTag, e.toString())
                            throw RuntimeException("Failed to preload $path in the APK's assets directory")
                        }
                    }
                }
            }

        copyAssetsToExternalJob.invokeOnCompletion { throwable: Throwable? ->
            val msgPrefix = "copyAssetsToExternalJob has been"

            throwable?.run {
                Log.e(logTag, "$msgPrefix canceled.")
            } ?: Log.i(logTag, "$msgPrefix completed.")
        }

        return super.onCreate()
    }
}
