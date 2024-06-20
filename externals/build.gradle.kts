import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.channels.Channels
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.isDirectory
import kotlin.io.path.Path
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.jetbrains.kotlin.daemon.common.toHexString
import xyz.ronella.gradle.plugin.simple.git.task.*

plugins {
    base
    id(libs.plugins.xyz.simple.git.get().pluginId)
}

tasks {
    fun File.sha256sum(): String {
        val sha256sum = MessageDigest.getInstance("SHA-256")
        val digest = sha256sum.digest(this.readBytes())

        return digest.toHexString()
    }

    fun downloadFile(url: URL, outputFileName: String) {
        url.openStream().use {
            Channels.newChannel(it).use { rbc ->
                FileOutputStream(outputFileName).use { fos ->
                    fos.channel.transferFrom(rbc, 0, Long.MAX_VALUE)
                }
            }
        }
    }

    fun XZCompressorInputStream.toFile(filePath: String) {
        File(filePath).outputStream().use { fileOutput -> this.copyTo(fileOutput) }
    }

    val downloadablePath = Path("$projectDir/${project.properties["dir.downloadable"]}")

    //FIXME Revise project.properties
    // - remove dir.gstAndroid/dir.tfliteAndroid if not necessary
    // - remove dir.downloadable if not necessary
    // - set enabled by using the value in local.properties?
    data class Downloadable(
        val tarFileName: String,
        val targetDir: String,
        val url: String,
        val enabled: Boolean,
        val downloadableFormat: String = "",
        val digestFileName: String = ""
    )

    val downloadables = mutableListOf<Downloadable>()

    // GStreamer Android Universal
    val gstVersion = try {
        libs.versions.gstreamer.get()
    } catch(e: IllegalStateException) {
        error("Failed to resolve the variable that defines the required GStreamer version.")
    }

    when {
        !project.hasProperty("dir.gstAndroid") -> {
            error("Could not find the project property, 'dir.gstAndroid'.")
        }
        project.properties["dir.gstAndroid"].toString().isBlank() -> {
            error("The project property, 'dir.gstAndroid', is set to an invalid value.")
        }
    }

    val gstDownloadable =
        Downloadable(
            "gstreamer-1.0-android-universal-$gstVersion.tar",
            project.properties["dir.gstAndroid"].toString(),
            "https://gstreamer.freedesktop.org/pkg/android/$gstVersion",
            true,
            ".xz",
            "gstreamer-1.0-android-universal-$gstVersion.tar.xz.sha256sum"
        )
    downloadables.add(gstDownloadable)

    // TensorFlow Lite
    /**
     * TODO: Using the alias in the version catalog makes a build-time dependency on libs.versions.something.
     *       For the optional features, it is better to define a property and use the value from the property.
     */
    val tfliteVersion = libs.versions.tensorflowLite.get()
    val enabledTFLite = when {
        !project.hasProperty("dir.tfliteAndroid") -> {
            false
        }
        project.properties["dir.tfliteAndroid"].toString().isBlank() -> {
            false
        }
        else -> {
            true
        }
    }

    val tfliteDownloadable =
        Downloadable(
            "tensorflow-lite-$tfliteVersion.tar",
            project.properties["dir.tfliteAndroid"].toString(),
            "https://raw.githubusercontent.com/nnstreamer/nnstreamer-android-resource/master/external",
            enabledTFLite,
            ".xz"
        )
    downloadables.add(tfliteDownloadable)


    register("prepareDownloadable") {
        if (!downloadablePath.isDirectory()) {
            @OptIn(ExperimentalPathApi::class) downloadablePath.deleteRecursively()
            downloadablePath.createDirectories()
        }

        for (downloadable in downloadables) {
            val (tarFileName, targetDir, url, isEnabled, downloadableFormat, digestFileName) = downloadable
            val targetPath = projectDir.toPath().resolve(targetDir)

            if (!isEnabled || targetPath.isDirectory()) {
                continue
            }

            println("Could not find $targetPath")
            println("...downloading from $url")
            println("This step may take some time to complete...")

            val downloadableName = "$tarFileName$downloadableFormat"
            downloadFile(URL("$url/$downloadableName"), downloadablePath.resolve(downloadableName).toString())

            val verified = if (digestFileName.isEmpty().or(digestFileName.isBlank())) {
                true
            } else {
                val path = downloadablePath.resolve(digestFileName)
                val fileNameToHashMap: MutableMap<String, String> = mutableMapOf()

                downloadFile(URL("$url/$digestFileName"), path.toString())

                path.toFile().forEachLine { line ->
                    val (hash, filename) = line.split("\\s+".toRegex())

                    fileNameToHashMap[filename] = hash
                }
                //FIXME Support other algorithms such as MD5, sha512
                fileNameToHashMap[downloadableName] == File("$downloadablePath/$downloadableName").sha256sum()
            }

            if (!verified) {
                throw IOException("Failed to verify the integrity of the downloaded file: $downloadableName")
            }

            when {
                downloadableName.endsWith(".xz") -> {
                    try {
                        File("$downloadablePath/$downloadableName").inputStream().buffered().use { bufferedIn ->
                            XZCompressorInputStream(bufferedIn).toFile("$downloadablePath/$tarFileName")
                        }
                        downloadablePath.resolve(downloadableName).deleteIfExists()
                    } catch (e: IOException) {
                        println("Failed to decompress $downloadablePath/$downloadableName")
                    }
                }
            }
        }
    }


    register("copyFromTar") {
        doLast {
            for (downloadble in downloadables) {
                val (tarFileName, targetDir, _, isEnabled) = downloadble

                if (!isEnabled) {
                    continue
                }

                val tarPath = downloadablePath.resolve(tarFileName)
                val tree = tarTree(tarPath)
                var intoPath = projectDir.toPath().resolve(targetDir)

                if (intoPath.exists()) {
                    continue
                }

                tree.visit {
                    if (this.relativePath.startsWith(targetDir)) {
                        intoPath = projectDir.toPath()
                    }
                    return@visit
                }

                copy {
                    from(tree)
                    into(intoPath)
                }

                tarPath.deleteIfExists()
            }
        }
        dependsOn("prepareDownloadable")
    }

    //TODO Use a specific commit ID or TAG
    register("initGitSubmodules", GitTask::class) { command = "submodule init" }

    register("checkoutGitSubmodules", GitTask::class) {
        command = "submodule foreach"
        args = listOf("git checkout .") // The git command arguments
        dependsOn("initGitSubmodules")
    }

    register("updateGitSubmodules", GitTask::class) {
        command = "submodule update --remote --recursive"
        dependsOn("checkoutGitSubmodules")
    }


    register("cleanAll", Delete::class) {
        delete(downloadablePath)
        for (downloadble in downloadables) {
            val (_, targetDir, _) = downloadble

            delete(projectDir.toPath().resolve(targetDir))
        }

        dependsOn("clean")
        mustRunAfter(":nnstreamer-api:cleanAll")
    }
}

tasks.named("build") {
    dependsOn("copyFromTar")
    dependsOn("updateGitSubmodules")
}
