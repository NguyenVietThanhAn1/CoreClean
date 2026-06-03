import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    jacoco
}

// Room: nơi xuất schema JSON cho migration
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace   = "com.coreclean.app"
    compileSdk  = 36

    defaultConfig {
        applicationId = "com.coreclean.app"
        minSdk        = 26
        targetSdk     = 36
        versionCode   = 1
        versionName   = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = gradleLocalProperties(rootDir, providers)
        buildConfigField("String", "SENTRY_DSN",
            "\"${localProps.getProperty("SENTRY_DSN", "")}\"")
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
        buildConfig = true
    }

    val localProps = gradleLocalProperties(rootDir, providers)
    // Resolve a signing property: local.properties first, then environment variable.
    // CI passes these via GitHub Secrets; local dev uses keystore.properties or local.properties.
    fun signingProp(key: String): String? =
        localProps.getProperty(key)?.takeIf { it.isNotBlank() }
            ?: System.getenv(key)?.takeIf { it.isNotBlank() }

    signingConfigs {
        if (signingProp("KEYSTORE_PATH") != null) {
            create("release") {
                storeFile     = file(signingProp("KEYSTORE_PATH")!!)
                storePassword = signingProp("KEYSTORE_PASSWORD") ?: ""
                keyAlias      = signingProp("KEY_ALIAS") ?: ""
                keyPassword   = signingProp("KEY_PASSWORD") ?: ""
            }
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("gms") {
            dimension = "distribution"
            isDefault = true
        }
        create("foss") {
            dimension = "distribution"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled    = true
            isShrinkResources  = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning != null) signingConfig = releaseSigning
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }


    lint {
        baseline = file("lint-baseline.xml")
        // Dependency version bumps are a deliberate release decision, not a code bug
        disable += "GradleDependency"
        // Some deps are inline strings intentionally (BOM + debug-only); TOML migration is tracked
        disable += "UseTomlInstead"
        // AGP version is pinned to a known-good version; upgrades are intentional
        disable += "AndroidGradlePluginVersion"
        // SelectedPhotoAccess: READ_MEDIA_IMAGES + partial access handled per UI flow
        disable += "SelectedPhotoAccess"
        // QueryPermissionsNeeded: getInstalledPackages() is used for app-size estimation only;
        // returns partial results on API 30+ which is acceptable (best-effort).
        disable += "QueryPermissionsNeeded"
        abortOnError = false
        warningsAsErrors = false
    }
}

dependencies {
    // ── Compose ──────────────────────────────────────────────────
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
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

    // ── AppCompat (locale switching) ──────────────────────────────
    implementation(libs.appcompat)

    // ── DataStore Preferences ─────────────────────────────────────
    implementation("androidx.datastore:datastore-preferences:1.1.3")

    // ── Sentry crash reporting (gms flavor only) ─────────────────
    "gmsImplementation"(libs.sentry.android)

    // ── Baseline profile installer ────────────────────────────────
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    // ── Window size class (tablet/foldable) ───────────────────────
    implementation("androidx.compose.material3:material3-window-size-class")

    // ── Serialization (type-safe Navigation) ──────────────────────
    implementation(libs.serialization.json)

    // ── Testing ───────────────────────────────────────────────────
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation(libs.work.testing)
    testImplementation(libs.robolectric)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("io.mockk:mockk-android:1.13.12")
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoTestReport/html"))
    }

    val fileFilter = listOf(
        "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test*.*", "android/**/*.*",
        "**/*_Factory*.*", "**/*_HiltModules*.*", "**/*_MembersInjector*.*",
        "**/Hilt_*.*", "**/*Module_*.*"
    )
    val debugTree = fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug") {
        exclude(fileFilter)
    }
    val kotlinDebugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    classDirectories.setFrom(files(debugTree, kotlinDebugTree))
    sourceDirectories.setFrom(files("$projectDir/src/main/java", "$projectDir/src/main/kotlin"))
    executionData.setFrom(fileTree(layout.buildDirectory.get()) {
        include("jacoco/testDebugUnitTest.exec", "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    })
}