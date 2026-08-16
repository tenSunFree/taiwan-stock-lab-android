plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room3)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.sun.taiwan_stock_lab_android.feature.stocklist"
    compileSdk = 37
    defaultConfig {
        minSdk = 24
    }
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.network)
    api(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.bundled)
    ksp(libs.androidx.room3.compiler)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.hilt.android)
    implementation(libs.material)
    implementation(projects.core.ui)  // 顏色資源
    implementation(libs.androidx.recyclerview)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit5.jupiter.api)
    testImplementation(libs.junit5.jupiter.params)
    testRuntimeOnly(libs.junit5.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}