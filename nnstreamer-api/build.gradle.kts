@file:Suppress("UnstableApiUsage")

import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory

plugins {
    id(libs.plugins.androidLibrary.get().pluginId)
    id(libs.plugins.jetbrainsDokka.get().pluginId)
}

android {
    val externalDirPath by rootProject.extra {
        project.rootDir.toPath().resolve(properties["dir.externals"].toString())
    }
    val gstDir = properties["dir.gstAndroid"].toString()
    val gstRootPath = externalDirPath.resolve(gstDir)

    val nnsDir = properties["dir.nnstreamer"].toString()
    val nnsRootPath = externalDirPath.resolve(nnsDir)

    val nnsEdgeDir = properties["dir.nnstreamerEdge"].toString()
    val nnsEdgeRootPath = externalDirPath.resolve(nnsEdgeDir)

    val mlApiDir = properties["dir.mlApi"].toString()
    val mlApiRootPath = externalDirPath.resolve(mlApiDir)
    val mlApiNNSJniPath = mlApiRootPath.resolve("java/android/nnstreamer/src/main/jni")

    namespace = "org.nnsuite.nnstreamer"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()
    ndkVersion = "25.2.9519653"

    defaultConfig {
        minSdk = 33 // The maximum API level supported by the NDK v25.2.9519653
        externalNativeBuild {
            ndkBuild {
                abiFilters("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                arguments(
                    "NDK_PROJECT_PATH=./",
                    "NDK_APPLICATION_MK=$mlApiNNSJniPath/Application.mk",
                    "GSTREAMER_JAVA_SRC_DIR=src/main/java",
                    "GSTREAMER_ROOT_ANDROID=$gstRootPath",
                    "NNSTREAMER_ROOT=$nnsRootPath",
                    "NNSTREAMER_EDGE_ROOT=$nnsEdgeRootPath",
                    "ML_API_ROOT=$mlApiRootPath"
                )
                targets("nnstreamer-native")

                // TODO: Remove code redundancies in enabling features
                if (project.hasProperty("dir.tfliteAndroid")) {
                    val tfliteDir = properties["dir.tfliteAndroid"].toString()

                    tfliteDir.also { dir ->
                        val rootPath = externalDirPath.resolve(dir)
                        val tfliteVersion = libs.versions.tensorflowLite.get()
                        val enableTflite = rootPath.isDirectory()

                        /**
                         * TODO: The app has a dependency on TFLite.
                         */
                        arguments("ENABLE_TF_LITE=$enableTflite")
                        if (!enableTflite) {
                            val msg =
                                "The property, 'dir.tfliteAndroid', is specified in 'gradle.properties', " +
                                        "but failed to resolve it to $rootPath. TFLite support will be disabled."
                            project.logger.lifecycle("WARNING: $msg")
                            return@also
                        }
                        arguments("TFLITE_ROOT_ANDROID=$rootPath")
                        arguments("TFLITE_VERSION=$tfliteVersion")
                    }
                }

                if (project.hasProperty("dir.snpe")) {
                    val snpeDir = properties["dir.snpe"].toString()

                    snpeDir.also { dir ->
                        val rootPath = externalDirPath.resolve(dir)
                        val enableSnpe = rootPath.isDirectory()

                        arguments("ENABLE_SNPE=$enableSnpe")
                        if (!enableSnpe) {
                            val msg =
                                "The property, 'dir.snpe', is specified in 'gradle.properties', " +
                                        "but failed to resolve it to $rootPath. SNPE support will be disabled."
                            project.logger.lifecycle("WARNING: $msg")
                            return@also
                        }

                        arguments("SNPE_DIR=$rootPath")
                        /**
                         * lib/aarch64-android is the default lib path for SNPE from v2.11 to v.2.19
                         * If it exists and is a directory, use it. Otherwise, SNPE_LIB_PATH is set by Android-snpe.mk.
                         */
                        if (rootPath.resolve("lib").resolve("aarch64-android").isDirectory()) {
                            arguments("SNPE_LIB_PATH=$rootPath/lib/aarch64-android")
                        }
                    }
                }

                if (project.hasProperty("dir.llama2c")) {
                    val llama2cDir = properties["dir.llama2c"].toString()

                    llama2cDir.also { dir ->
                        val rootPath = externalDirPath.resolve(dir)
                        val enableLlama2c = rootPath.isDirectory()

                        arguments("ENABLE_LLAMA2C=$enableLlama2c")
                        if (!enableLlama2c) {
                            val msg =
                                "The property, 'dir.llama2c', is specified in 'gradle.properties', " +
                                        "but failed to resolve it to $rootPath. llama2.c support will be disabled."
                            project.logger.lifecycle("WARNING: $msg")
                            return@also
                        }

                        arguments("LLAMA2C_DIR=$rootPath")
                    }
                }
            }
        }
    }
    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    externalNativeBuild {
        ndkBuild {
            path = file(mlApiNNSJniPath.resolve("Android.mk"))
        }
    }

    productFlavors {
    }

    // To do not show build warning messages
    packaging {
        jniLibs.keepDebugSymbols.add("*/arm64-v8a/*_skel.so")
    }

    val genNnsSrc = tasks.register("genNnsSrc", Copy::class) {
        val commonSuffix = "src/main/java/org/nnsuite/nnstreamer"
        val srcDirPath =
            externalDirPath.resolve("ml-api/java/android/nnstreamer").resolve(commonSuffix)
        val outDirPath = project.projectDir.toPath().resolve(commonSuffix).apply {
            createDirectories()
        }

        group = BasePlugin.BUILD_GROUP
        description = "Generates NNStreamer Java sources"

        from(srcDirPath) {
            include("*.java")
        }

        filter { line: String ->
            line.replace("@BUILD_ANDROID@", "")
        }

        into(outDirPath)

        filteringCharset = "UTF-8"
        outputs.upToDateWhen { false }
    }

    sourceSets {
        getByName("main") {
            java {
                srcDirs(genNnsSrc)
            }
        }
    }

    tasks {
        named("preBuild") {
            dependsOn("genNnsSrc")
        }

        register("cleanAll", Delete::class) {
            val generatedDirs = listOf("gst-android-build", "src")

            for (genDir in generatedDirs) {
                project.projectDir.toPath().resolve(genDir).apply {
                    delete(this)
                }
            }
            dependsOn("clean")
        }
    }

    dependencies {
        // Dokka
        implementation(libs.dokka.base)
        compileOnly(libs.dokka.core)
    }
}

afterEvaluate {
    listOf("Debug", "Release").forEach { buildType ->
        val compileJavaWithJavacTask =
            project.getTasksByName("compile${buildType}JavaWithJavac", false)
        val externalNativeBuildTask =
            project.getTasksByName("externalNativeBuild${buildType}", false)

        if (compileJavaWithJavacTask.size >= 1 && externalNativeBuildTask.size >= 1) {
            val compileJavaTask = compileJavaWithJavacTask.first()
            val nativeBuildTask = externalNativeBuildTask.first()

            compileJavaTask.dependsOn(nativeBuildTask)
        }
    }
}
