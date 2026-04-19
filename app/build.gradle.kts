plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace   = "com.cleaner.app"
    compileSdk  = 36

    defaultConfig {
        applicationId = "com.cleaner.app"
        minSdk        = 26
        targetSdk     = 36
        versionCode   = 1
        versionName   = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled    = true
            isShrinkResources  = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable        = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── Compose ──────────────────────────────────────────────────
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ── Navigation ───────────────────────────────────────────────
    implementation(libs.compose.navigation)

    // ── Material Components (Needed for XML themes) ───────────────
    implementation("com.google.android.material:material:1.12.0")

    // ── Lifecycle / ViewModel ────────────────────────────────────
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // ── Hilt DI ──────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.androidx.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)

    // ── Room ──────────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ── Coroutines ────────────────────────────────────────────────
    implementation(libs.coroutines.android)

    // ── WorkManager ───────────────────────────────────────────────
    implementation(libs.work.runtime)

    // ── Coil 3 ───────────────────────────────────────────────────
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    // ── Serialization (type-safe Navigation) ──────────────────────
    implementation(libs.serialization.json)

    // ── Testing ───────────────────────────────────────────────────
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}