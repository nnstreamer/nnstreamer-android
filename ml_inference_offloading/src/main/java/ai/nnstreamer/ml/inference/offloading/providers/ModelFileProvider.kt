package ai.nnstreamer.ml.inference.offloading.providers

import ai.nnstreamer.ml.inference.offloading.R
import android.content.Context
import androidx.core.content.FileProvider
import java.io.FileOutputStream
import java.io.IOException

class ModelFileProvider : FileProvider(R.xml.file_paths) {
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


        resAssets.list(path)?.run {
            filterNotNull().onEach { file ->
                val os = FileOutputStream(externalDir.resolve(file))

                resAssets.open("$path/$file").use { stream ->
                    stream.copyTo(os)
                }
            }
        }
    }

    @Override
    override fun onCreate(): Boolean {
        context?.run {
            assetPaths.onEach { path ->
                try {
                    copyAssetsToExternal(this, path)
                } catch (e: Exception) {
                    throw RuntimeException("Failed to preload $path in the APK's assets directory")
                }
            }
        }

        return super.onCreate()
    }

}
