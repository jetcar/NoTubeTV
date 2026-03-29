@file:Suppress("DEPRECATION")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jetcar.vidrox"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jetcar.vidrox"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "0.0.3"

    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    buildToolsVersion = "36.0.0"
    ndkVersion = "25.2.9519653"


}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation (libs.ktor.ktor.client.okhttp)
    api(libs.compose.webview.multiplatform) {
        // Exclude desktop-only JCEF/jogamp dependencies that can't be resolved from Android repos
        exclude(group = "org.jogamp.gluegen", module = "gluegen-rt")
        exclude(group = "org.jogamp.jogl", module = "jogl-all")
        exclude(group = "dev.datlag.kcef", module = "kcef")
        exclude(group = "dev.datlag", module = "kcef")
        exclude(group = "dev.datlag", module = "jcef")
    }
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3.android)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}