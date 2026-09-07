plugins {
    `java-library`
    alias(libs.plugins.kotlinJvm)
    jacoco
}

kotlin {
    jvmToolchain(17)
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
