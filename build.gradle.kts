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
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics.plugin) apply false
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
    tasks.withType<Detekt>().configureEach {
        reports {
            html.required.set(true) // human-readable, uploaded as a CI artifact
            checkstyle.required.set(true) // machine-readable, e.g. for future dashboards
            sarif.required.set(true) // optional: enables GitHub code scanning annotations
        }
    }
}

// Points Git at the tracked hooks in scripts/hooks/ via `core.hooksPath`, rather than copying
// them into .git/hooks/ (which isn't version-controlled and would drift out of sync with the
// tracked source whenever a hook is edited). One-time step per clone — see README → Local
// Development.
tasks.register("installGitHooks") {
    group = "git hooks"
    description = "Configures Git to use the tracked hooks in scripts/hooks."
    // Captured here (at configuration time) as plain File values, not inside doLast — the
    // configuration cache can't serialize a live rootProject/Project reference held by a task
    // action, only plain serializable values like File.
    val hooksDir = rootProject.file("scripts/hooks")
    val workingDir = rootProject.projectDir
    doLast {
        check(hooksDir.isDirectory) { "Git hooks directory not found: ${hooksDir.path}" }
        val process =
            ProcessBuilder("git", "config", "core.hooksPath", "scripts/hooks")
                .directory(workingDir)
                .inheritIO()
                .start()
        val exitCode = process.waitFor()
        check(exitCode == 0) { "Failed to configure Git core.hooksPath." }
        logger.lifecycle("Configured Git hooks path: scripts/hooks")
    }
}
