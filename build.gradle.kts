plugins {
    alias(libs.plugins.xyz.simple.git) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
}

buildscript {
    dependencies {
        classpath(libs.tukaani.xz)
    }
}
