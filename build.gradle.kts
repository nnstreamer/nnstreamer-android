plugins {
    alias(libs.plugins.xyz.simple.git) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.googleDevtoolsKsp) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.jetbrainsKotlinSerialization) apply false
    alias(libs.plugins.jetbrainsDokka) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

buildscript {
    dependencies {
        classpath(libs.tukaani.xz)
        classpath(libs.dokka.base)
    }
}

tasks {
    register("cleanAll") {
        dependsOn("nnstreamer-api:cleanAll")
        dependsOn("externals:cleanAll")
        dependsOn("ml_inference_offloading:cleanAll")
    }
}
