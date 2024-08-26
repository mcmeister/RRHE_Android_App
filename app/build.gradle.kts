plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // Keep kapt for Data Binding
    id("com.google.devtools.ksp") version "1.9.0-1.0.13" // Updated KSP plugin
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.rrhe"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.rrhe"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            keyAlias = "rrheAlias"
            keyPassword = "online"
            storeFile = file("/home/mcmeister/Documents/RRHE_App_Dev/keystore_rrhe.jks")
            storePassword = "online"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        dataBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Accompanist
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.pager)
    implementation(libs.accompanist.pager.indicators)

    // Square libraries
    implementation(libs.logging.interceptor)
    implementation(libs.gson)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.urlconnection)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // AndroidX libraries
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.runtime.ktx.v284)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx.v291)
    implementation(libs.androidx.emoji2)
    ksp(libs.androidx.room.compiler)

    // Material Components
    implementation(libs.material)

    // Glide
    implementation(libs.androidx.exifinterface)
    implementation(libs.glide)
    implementation(libs.okhttp3.integration)
    ksp(libs.glide.compiler)

    // Jetpack Compose
    implementation(libs.ui)
    implementation(libs.androidx.material)
    implementation(libs.material3)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.runtime.livedata)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // Navigation for Compose
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)

    // Hilt for ViewModel
    implementation(libs.androidx.hilt.navigation.compose)

    // ViewPager2
    implementation(libs.androidx.viewpager2)

    // Firebase
    implementation(libs.firebase.messaging)

    // QR Code
    implementation(libs.zxing.android.embedded)

    // Login
    implementation(libs.androidx.security.crypto)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v121)
    androidTestImplementation(libs.androidx.espresso.core.v361)
    androidTestImplementation(libs.androidx.runner)
}

apply(plugin = "com.google.gms.google-services")
