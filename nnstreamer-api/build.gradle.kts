@file:Suppress("UnstableApiUsage")

import kotlin.io.path.Path
import kotlin.io.path.createDirectories

plugins {
    id(libs.plugins.androidLibrary.get().pluginId)
}

android {
    val externalDirPath by rootProject.extra {
        project.rootDir.toPath().resolve(properties["dir.externals"].toString())
    }

    namespace = "org.nnsuite.nnstreamer"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()
    ndkVersion = "25.2.9519653"

    defaultConfig {
        minSdk = 33 // The maximum API level supported by the NDK v25.2.9519653
        externalNativeBuild {
            ndkBuild {
                abiFilters("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                arguments("NDK_PROJECT_PATH=./",
                        "NDK_APPLICATION_MK=$externalDirPath/ml-api/java/android/nnstreamer/src/main/jni/Application.mk",
                        "GSTREAMER_JAVA_SRC_DIR=src/main/java",
                        "GSTREAMER_ROOT_ANDROID=$externalDirPath/gst-1.0-android-universal",
                        "NNSTREAMER_ROOT=$externalDirPath/nnstreamer",
                        "NNSTREAMER_EDGE_ROOT=$externalDirPath/nnstreamer-edge",
                        "ML_API_ROOT=$externalDirPath/ml-api"
                )
                targets("nnstreamer-native")

                if (project.hasProperty("dir.tfliteAndroid")) {
                    arguments("TFLITE_ROOT_ANDROID=$externalDirPath/tensorflow-lite")
                }
            }
        }
    }
    buildTypes {
        debug {
            enableUnitTestCoverage=true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    externalNativeBuild {
        ndkBuild {
            path=file(project.projectDir.toPath().resolve("src/main/jni/Android.mk"))
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
        val srcDirPath = externalDirPath.resolve("ml-api/java/android/nnstreamer").resolve(commonSuffix)
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
        register("genJniSrc", Copy::class) {
            val srcDirPath = externalDirPath.resolve("ml-api/java/android/nnstreamer/src/main/jni")
            val outDirPath = project.projectDir.toPath().resolve("src/main/jni").apply {
                createDirectories()
            }

            group = BasePlugin.BUILD_GROUP
            description = "Generates NNStreamer JNI sources"

            from(srcDirPath)
            into(outDirPath)

            if (project.hasProperty("dir.tfliteAndroid")) {
                val tfliteVersion = libs.versions.tensorflowLite.get()
                filter { line: String ->
                    line.replace("TFLITE_VERSION := 2.8.1", "TFLITE_VERSION := $tfliteVersion")
                }
            }
            filteringCharset = "UTF-8"
        }

        named("preBuild") {
            dependsOn("genNnsSrc")
            dependsOn("genJniSrc")
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
}

afterEvaluate {
    listOf("Debug", "Release").forEach { buildType ->
        val compileJavaWithJavacTask = project.getTasksByName("compile${buildType}JavaWithJavac", false)
        val externalNativeBuildTask = project.getTasksByName("externalNativeBuild${buildType}", false)

        if (compileJavaWithJavacTask.size >= 1 && externalNativeBuildTask.size >= 1) {
            val compileJavaTask = compileJavaWithJavacTask.first()
            val nativeBuildTask = externalNativeBuildTask.first()

            compileJavaTask.dependsOn(nativeBuildTask)
        }
    }
}
