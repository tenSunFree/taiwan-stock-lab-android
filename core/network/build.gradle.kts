plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sun.taiwan_stock_lab_android.core.network"
    compileSdk = 37
    defaultConfig {
        minSdk = 24
    }
    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(libs.retrofit.core)
    api(libs.moshi.core)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    testImplementation(libs.junit)
}
