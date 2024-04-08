@file:Suppress("UnstableApiUsage")

import kotlin.io.path.Path

plugins {
    alias(libs.plugins.androidLibrary)
}

val externalDirPath by rootProject.extra { rootDir.path + "/" + properties["dir.externals"] }

android {
    val pathExternals = findProperty("externalDirPath").toString()
    val relativePathExternals = projectDir.toPath().relativize(Path(pathExternals))

    namespace = "org.nnsuite.nnstreamer"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()
    ndkVersion = "25.2.9519653"

    defaultConfig {
        minSdk = 21
        externalNativeBuild {
            ndkBuild {
                abiFilters("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                arguments(
                        "NDK_PROJECT_PATH=./",
                        "NDK_APPLICATION_MK=Application.mk",
                        "GSTREAMER_JAVA_SRC_DIR=src/main/java",
                        "GSTREAMER_ROOT_ANDROID=$relativePathExternals/gst-1.0-android-universal",
                        "NNSTREAMER_ROOT=$relativePathExternals/nnstreamer",
                        "ML_API_ROOT=$relativePathExternals/ml-api"
                )
                targets("nnstreamer_api")
            }
        }
    }
    externalNativeBuild {
        ndkBuild {
            path=file("./Android.mk")
        }
    }
}
