plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
}

// Firebase is optional for this public repository.
//
// A local developer who places `google-services.json` in this module gets Crashlytics +
// Analytics. Contributors and CI without that project-specific configuration still build and run
// the app normally — Firebase is omitted entirely rather than included in a half-configured state.
val firebaseConfigured = file("google-services.json").isFile

// Applied only when local Firebase configuration exists. Plugin versions are declared in the root
// build.gradle.kts with `apply false`, so they're available here without duplicating them.
if (firebaseConfigured) {
    pluginManager.apply("com.google.gms.google-services")
    pluginManager.apply("com.google.firebase.crashlytics")
}

android {
    namespace = "com.sun.taiwan_stock_lab_android"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.sun.taiwan_stock_lab_android"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "com.sun.taiwan_stock_lab_android.HiltTestRunner"
    }
    buildTypes {
        debug {
            // Keep local development crashes out of production Crashlytics metrics. Read by the
            // firebase_crashlytics_collection_enabled meta-data in AndroidManifest.xml; unused
            // when Firebase isn't configured at all.
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "false"
        }
        release {
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(projects.feature.stocklist)
    implementation(projects.core.network)
    implementation(projects.core.ui)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // Firebase is intentionally absent from Firebase-less builds — avoids shipping or
    // initializing a partially configured SDK when google-services.json isn't present.
    if (firebaseConfigured) {
        implementation(platform(libs.firebase.bom))
        implementation(libs.firebase.crashlytics)
        implementation(libs.firebase.analytics)
    }
    // LeakCanary must never be included in release builds.
    debugImplementation(libs.leakcanary.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
