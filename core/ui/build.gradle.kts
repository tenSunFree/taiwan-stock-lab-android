plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sun.taiwan_stock_lab_android.core.ui"
    compileSdk = 37
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}