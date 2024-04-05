@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
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
