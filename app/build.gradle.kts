plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.google.service)
    alias(libs.plugins.hilt.android)
    kotlin("kapt")
}

android {
    namespace = "com.chessflow.jni"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.chessflow.jni"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17","-DANDROID","-D__ANDROID__", "-fexceptions", "-frtti")
                arguments("-DANDROID_STL=c++_shared","-DANDROID_ARM_NEON=TRUE")
            }
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = false
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // 🔹 JNI .so файловете
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    kapt {
        correctErrorTypes = true
    }

    // 🔹 Версия на NDK
    ndkVersion = "27.0.12077973"
}

dependencies {
    // Android core
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.play.services.auth)

    // Navigation & Gson
    implementation(libs.androidx.navigation.compose)
    implementation(libs.gson)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.foundation.layout.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.javapoet)

    // Lifecycle
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // DataStore
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.kt)

    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.window)
    implementation(libs.androidx.window.core)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.debugImplementation)
}
