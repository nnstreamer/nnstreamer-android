@file:Suppress("UnstableApiUsage")

include(":ml_inference_offloading")


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
