import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidx.room3) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "dev.detekt")
    configure<KtlintExtension> {
        ignoreFailures.set(false)
        outputToConsole.set(true)
        reporters {
            reporter(ReporterType.PLAIN)
            reporter(ReporterType.CHECKSTYLE)
        }
    }
    configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        // No baseline configured yet; decide after reviewing findings
    }
    // Explicit report formats so CI has stable, predictable paths to collect as artifacts.
    tasks.withType<Detekt>().configureEach {
        reports {
            html.required.set(true) // human-readable, uploaded as a CI artifact
            sarif.required.set(true) // machine-readable, enables GitHub code scanning annotations
        }
    }
}
