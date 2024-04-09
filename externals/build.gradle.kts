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

    register("prepareDownloadable") {
        if (!downloadablePath.isDirectory()) {
            downloadablePath.createDirectories()
        }

        //gstreamer-1.0-android-universal
        val gstXzFileName = "$gstTarFileName.xz"
        val gstAndroidUniversalUrl = URL("https://gstreamer.freedesktop.org/pkg/android/$gstVersion/$gstXzFileName")

        if (!Path("$downloadablePath/$gstTarFileName").isRegularFile()) {
            println("Could not find $gstTarFileName")
            println("...downloading from https://gstreamer.freedesktop.org/pkg/android")
            println("This step may take some time to complete...")
            downloadFile(gstAndroidUniversalUrl, "$downloadablePath/$gstXzFileName")

            try {
                File("$downloadablePath/$gstXzFileName").inputStream().buffered().use { bufferedIn ->
                    XZCompressorInputStream(bufferedIn).toFile("$downloadablePath/$gstTarFileName")
                }
            } catch (e: java.io.IOException) {
                println("Failed to decompress $downloadablePath/$gstXzFileName")
            } finally {
                Path("$downloadablePath/$gstXzFileName").deleteIfExists()
            }
        }
    }

    register("copyFromTar", Copy::class) {
        from(tarTree("${downloadablePath.toUri()}/$gstTarFileName"))
        into(gstAndroidPath)

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
