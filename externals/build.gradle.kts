import org.apache.commons.compress.compressors.xz.XZCompressorInputStream

import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import kotlin.io.path.*
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

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
    val downloadablePath = Path("${projectDir}/${project.properties["dir.downloadable"].toString()}")
    val gstVersion = libs.versions.gstreamer.get()
    val gstTarFileName = "gstreamer-1.0-android-universal-$gstVersion.tar"
    val gstAndroidPath = Path("${projectDir}/${project.properties["dir.gstAndroid"].toString()}")
    val gstAndroidUniversalUrl = "https://gstreamer.freedesktop.org/pkg/android/$gstVersion"

    val fileList = mutableListOf(
        Triple(gstTarFileName, gstAndroidPath, gstAndroidUniversalUrl)
    )

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
        File(filePath).outputStream().use { fileOutput ->
            this.copyTo(fileOutput)
        }
    }

    fun prepareTflite() {
        val tfliteVersion = libs.versions.tensorflowLite.get()
        val tfliteTarFileName = "tensorflow-lite-$tfliteVersion.tar"
        val tfliteAndroidPath = Path("${projectDir}/${project.properties["dir.tfliteAndroid"].toString()}")
        val tfliteUrl = "https://raw.githubusercontent.com/nnstreamer/nnstreamer-android-resource/master/external"

        fileList.add(Triple(tfliteTarFileName, tfliteAndroidPath, tfliteUrl))
    }

    register("prepareDownloadable") {
        if (project.hasProperty("dir.tfliteAndroid")) {
            prepareTflite()
        }

        if (!downloadablePath.isDirectory()) {
            downloadablePath.createDirectories()
        }

        for (file in fileList) {
            val tarFileName = file.first
            val androidPath = file.second
            val url = file.third
            val xzFileName = "$tarFileName.xz"

            if (!Path("$androidPath").isDirectory()) {
                println("Could not find $androidPath")
                println("...downloading from $url")
                println("This step may take some time to complete...")
                downloadFile(URL("$url/$xzFileName"), "$downloadablePath/$xzFileName")

                try {
                    File("$downloadablePath/$xzFileName").inputStream().buffered().use { bufferedIn ->
                        XZCompressorInputStream(bufferedIn).toFile("$downloadablePath/$tarFileName")
                    }
                } catch (e: java.io.IOException) {
                    println("Failed to decompress $downloadablePath/$xzFileName")
                } finally {
                    Path("$downloadablePath/xzFileName").deleteIfExists()
                }
            }
        }
    }

    register("copyFromTar") {
        doLast {
            for (file in fileList) {
                val tarFileName = file.first
                val androidPath = file.second
                if (!Path("$androidPath").isDirectory()) {
                    copy {
                        from(tarTree("${downloadablePath.toUri()}/$tarFileName"))
                        into(androidPath)
                    }
                }
                Path("$downloadablePath/$tarFileName").deleteIfExists()
            }
        }

        dependsOn("prepareDownloadable")
    }

    register("initGitSubmodules", GitTask::class) {
        command = "submodule init"
    }

    register("updateGitSubmodules", GitTask::class) {
        command = "submodule update"

        dependsOn("initGitSubmodules")
    }

    register("checkoutGitSubmodules", GitTask::class) {
        command = "submodule foreach"
        args = listOf("git checkout .") //The git command arguments
        dependsOn("updateGitSubmodules")
    }
}

tasks.named("build") {
    dependsOn("copyFromTar")
    dependsOn("checkoutGitSubmodules")
}
