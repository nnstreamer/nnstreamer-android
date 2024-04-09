@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

rootProject.name = "nnstreamer-android"

include(":externals")
include(":nnstreamer-api")
include(":ml_inference_offloading")
