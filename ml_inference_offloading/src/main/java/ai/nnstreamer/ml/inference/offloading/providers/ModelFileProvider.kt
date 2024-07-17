package ai.nnstreamer.ml.inference.offloading.providers

import ai.nnstreamer.ml.inference.offloading.App
import ai.nnstreamer.ml.inference.offloading.R
import ai.nnstreamer.ml.inference.offloading.data.OfflineModelsRepository
import android.content.Context
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject


class ModelFileProvider : FileProvider(R.xml.file_paths) {
    private val logTag = "FileProvider"

    @Inject
    lateinit var modelsRepository: OfflineModelsRepository

    private lateinit var copyAssetsToExternalJob: Job

    companion object {
        val assetPaths = listOf("models")
    }

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

        resAssets.list(path)?.filterNotNull()?.takeWhile {
            // Copy the bundled asset files if they do not exist in the private external storage
            !externalDir.resolve(it).exists()
        }?.onEach { name ->
            resAssets.open("$path/$name").use { stream ->
                stream.copyTo(FileOutputStream(externalDir.resolve(name)))
            }
        }
    }


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
