import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import kotlin.io.path.*
import kotlin.io.path.Path
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import xyz.ronella.gradle.plugin.simple.git.task.*

apply {
    plugin("base")
    plugin(libs.plugins.xyz.simple.git.get().pluginId)
}

buildscript {
    dependencies {
        classpath(libs.commons.compress)
        classpath(libs.tukaani.xz)
    }

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

tasks {
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

    val downloadablePath = Path("${projectDir}/${project.properties["dir.downloadable"].toString()}")

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
    )

    val downloadables = mutableListOf<Downloadable>()

    // GStreamer Android Universal
    val gstVersion = libs.versions.gstreamer.get()
    val gstDownloadable =
        Downloadable(
            "gstreamer-1.0-android-universal-$gstVersion.tar",
            project.properties["dir.gstAndroid"].toString(),
            "https://gstreamer.freedesktop.org/pkg/android/$gstVersion",
            true,
            ".xz"
        )
    downloadables.add(gstDownloadable)

    // TensorFlow Lite
    val tfliteVersion = libs.versions.tensorflowLite.get()
    val tfliteDownloadable =
        Downloadable(
            "tensorflow-lite-$tfliteVersion.tar",
            project.properties["dir.tfliteAndroid"].toString(),
            "https://raw.githubusercontent.com/nnstreamer/nnstreamer-android-resource/master/external",
            true,
            ".xz"
        )
    downloadables.add(tfliteDownloadable)


    register("prepareDownloadable") {
        if (!downloadablePath.isDirectory()) {
            @OptIn(ExperimentalPathApi::class) downloadablePath.deleteRecursively()
            downloadablePath.createDirectories()
        }

        for (downloadble in downloadables) {
            val (tarFileName, targetDir, url) = downloadble
            val targetPath = projectDir.toPath().resolve(targetDir)

            if (targetPath.isDirectory()) {
                continue
            }

            println("Could not find $targetPath")
            println("...downloading from $url")
            println("This step may take some time to complete...")

            val downloadbleName = "$tarFileName${downloadble.downloadableFormat}"
            downloadFile(URL("$url/$downloadbleName"), downloadablePath.resolve(downloadbleName).toString())

            when {
                downloadbleName.endsWith(".xz") -> {
                    try {
                        File("$downloadablePath/$downloadbleName").inputStream().buffered().use { bufferedIn ->
                            XZCompressorInputStream(bufferedIn).toFile("$downloadablePath/$tarFileName")
                        }
                        downloadablePath.resolve(downloadbleName).deleteIfExists()
                    } catch (e: java.io.IOException) {
                        println("Failed to decompress $downloadablePath/$downloadbleName")
                    }
                }
            }
        }
    }


    register("copyFromTar") {
        doLast {
            for (downloadble in downloadables) {
                val (tarFileName, targetDir, _) = downloadble
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

    register("updateGitSubmodules", GitTask::class) {
        command = "submodule update"

        dependsOn("initGitSubmodules")
    }

    register("checkoutGitSubmodules", GitTask::class) {
        command = "submodule foreach"
        args = listOf("git checkout .") // The git command arguments
        dependsOn("updateGitSubmodules")
    }

    register("cleanAll", Delete::class) {
        delete(downloadablePath)
        for (downloadble in downloadables) {
            val (_, targetDir, _) = downloadble

            delete(projectDir.toPath().resolve(targetDir))
        }

        dependsOn("clean")
    }
}

tasks.named("build") {
    dependsOn("copyFromTar")
    dependsOn("checkoutGitSubmodules")
}


