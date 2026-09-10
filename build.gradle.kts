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

data class CoverageModule(val name: String, val xmlFile: File, val htmlDirectory: File)

data class Counts(val missed: Long = 0, val covered: Long = 0) {
    val total: Long get() = missed + covered
    val percentage: Double get() = if (total == 0L) 0.0 else covered * 100.0 / total

    /** How many more lines/branches must flip from missed→covered to reach [targetPercent] (0..100). */
    fun neededFor(targetPercent: Double): Long {
        if (total == 0L) return 0
        val required = kotlin.math.ceil(total * (targetPercent / 100.0) - covered).toLong()
        return required.coerceAtLeast(0)
    }
}

data class ModuleResult(
    val name: String,
    val line: Counts,
    val branch: Counts,
    val instruction: Counts,
    val available: Boolean,
)

// ---------------------------------------------------------------------------
// Project JVM coverage dashboard
//
// Aggregates each module's JaCoCo counters into ONE local overview page so
// you don't have to open 5 separate module reports.
//
// IMPORTANT: this is a RAW JaCoCo aggregate computed directly from the XML
// counters. It is NOT guaranteed to equal Codecov's reported project
// percentage, because Codecov additionally applies the `ignore:` rules in
// codecov.yml (Room *_Impl, ViewBinding, Hilt-generated code, etc.), which
// this script does not know about. Use this dashboard to find low-coverage
// hotspots locally and to sanity-check trends between runs — treat Codecov's
// number on the PR check as the source of truth for the "official" 70% goal.
// ---------------------------------------------------------------------------
tasks.register("aggregateCoverageReport") {
    group = "verification"
    description = "..."
    notCompatibleWithConfigurationCache(
        "The data types used by the aggregate report cannot be safely serialized into the Configuration Cache; this task does not need to be cached in the first place."
    )
    dependsOn(
        ":app:createDebugUnitTestCoverageReport",
        ":core:network:createDebugUnitTestCoverageReport",
        ":core:ui:createDebugUnitTestCoverageReport",
        ":core:common:jacocoTestReport",
        ":feature:stocklist:createDebugUnitTestCoverageReport",
    )
    val modules = listOf(
        CoverageModule(
            "app",
            file("app/build/reports/coverage/test/debug/report.xml"),
            file("app/build/reports/coverage/test/debug"),
        ),
        CoverageModule(
            "core-network",
            file("core/network/build/reports/coverage/test/debug/report.xml"),
            file("core/network/build/reports/coverage/test/debug"),
        ),
        CoverageModule(
            "core-ui",
            file("core/ui/build/reports/coverage/test/debug/report.xml"),
            file("core/ui/build/reports/coverage/test/debug"),
        ),
        CoverageModule(
            "core-common",
            file("core/common/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"),
            file("core/common/build/reports/jacoco/jacocoTestReport/html"),
        ),
        CoverageModule(
            "feature-stocklist",
            file("feature/stocklist/build/reports/coverage/test/debug/report.xml"),
            file("feature/stocklist/build/reports/coverage/test/debug"),
        ),
    )
    val outputDirectory = layout.buildDirectory.dir("reports/coverage-aggregate")
    doLast {
        val output = outputDirectory.get().asFile
        output.deleteRecursively()
        output.mkdirs()
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
            isValidating = false
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        }
        val builder = factory.newDocumentBuilder()
        // report.xml references report.dtd; we don't need to actually fetch it offline.
        builder.setEntityResolver { _, _ -> org.xml.sax.InputSource(java.io.StringReader("")) }
        fun readCounters(xmlFile: File): Triple<Counts, Counts, Counts> {
            val document = builder.parse(xmlFile)
            var lines = Counts()
            var branches = Counts()
            var instructions = Counts()
            // Only the <report> element's DIRECT <counter> children are the module-wide totals —
            // <package>/<class>/<method> nodes also have <counter> children that must NOT be summed in.
            val children = document.documentElement.childNodes
            for (i in 0 until children.length) {
                val node = children.item(i)
                if (node.nodeName != "counter") continue
                val attrs = node.attributes
                val type = attrs.getNamedItem("type").nodeValue
                val missed = attrs.getNamedItem("missed").nodeValue.toLong()
                val covered = attrs.getNamedItem("covered").nodeValue.toLong()
                val counts = Counts(missed, covered)
                when (type) {
                    "LINE" -> lines = counts
                    "BRANCH" -> branches = counts
                    "INSTRUCTION" -> instructions = counts
                }
            }
            return Triple(lines, branches, instructions)
        }

        val results = modules.map { module ->
            if (!module.xmlFile.exists()) {
                logger.warn("Coverage XML missing for ${module.name}: ${module.xmlFile.path}")
                ModuleResult(module.name, Counts(), Counts(), Counts(), available = false)
            } else {
                val (line, branch, instruction) = readCounters(module.xmlFile)
                if (module.htmlDirectory.exists()) {
                    copy {
                        from(module.htmlDirectory)
                        into(output.resolve(module.name))
                    }
                }
                ModuleResult(module.name, line, branch, instruction, available = true)
            }
        }

        fun aggregate(selector: (ModuleResult) -> Counts) = Counts(
            missed = results.sumOf { selector(it).missed },
            covered = results.sumOf { selector(it).covered },
        )

        val totalLines = aggregate { it.line }
        val totalBranches = aggregate { it.branch }
        val totalInstructions = aggregate { it.instruction }
        val linesNeededFor70 = totalLines.neededFor(70.0)
        fun pct(v: Double) = String.format(java.util.Locale.US, "%.2f%%", v)
        fun statusClass(v: Double) = when {
            v >= 70.0 -> "good"
            v >= 50.0 -> "warning"
            else -> "bad"
        }

        val rows = results.joinToString("\n") { r ->
            if (!r.available) {
                """<tr><td>${r.name}</td><td colspan="4" class="missing">Coverage report missing — run the module's coverage task first</td></tr>"""
            } else {
                """
                <tr>
                  <td><a href="${r.name}/index.html">${r.name}</a></td>
                  <td class="${statusClass(r.line.percentage)}">${pct(r.line.percentage)} (${r.line.covered}/${r.line.total})</td>
                  <td>${pct(r.branch.percentage)} (${r.branch.covered}/${r.branch.total})</td>
                  <td>${pct(r.instruction.percentage)}</td>
                  <td><div class="bar"><div class="fill ${statusClass(r.line.percentage)}-bar" style="width:${r.line.percentage}%"></div></div></td>
                </tr>
                """.trimIndent()
            }
        }
        output.resolve("index.html").writeText(
            """
            <!DOCTYPE html>
            <html lang="zh-Hant">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <title>Project JVM Coverage (raw, local)</title>
              <style>
                body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;max-width:1100px;margin:40px auto;padding:0 20px;color:#202124}
                h1{margin-bottom:8px}
                .coverage{font-size:48px;font-weight:700}
                .good{color:#188038} .warning{color:#b06000} .bad{color:#d93025}
                .metadata{margin-top:8px;color:#5f6368}
                .notice{margin-top:20px;padding:14px;background:#f8f9fa;border-left:4px solid #5f6368}
                table{width:100%;border-collapse:collapse;margin-top:30px}
                th,td{padding:12px;border-bottom:1px solid #ddd;text-align:left}
                th{background:#f8f9fa}
                .bar{width:180px;height:10px;background:#e8eaed;border-radius:5px;overflow:hidden}
                .fill{height:100%}
                .good-bar{background:#188038} .warning-bar{background:#f9ab00} .bad-bar{background:#d93025}
                .missing{color:#9aa0a6}
              </style>
            </head>
            <body>
              <h1>Project JVM Coverage</h1>
              <div class="coverage ${statusClass(totalLines.percentage)}">${pct(totalLines.percentage)}</div>
              <div class="metadata">
                Lines: ${totalLines.covered}/${totalLines.total} &nbsp;|&nbsp;
                Branch: ${pct(totalBranches.percentage)} &nbsp;|&nbsp;
                Instruction: ${pct(totalInstructions.percentage)} &nbsp;|&nbsp;
                Target: 70% &nbsp;|&nbsp;
                <strong>還差 $linesNeededFor70 行</strong> 才能到 70%
              </div>
              <div class="notice">
                這是純本地 JaCoCo 原始加總，<b>不等於 Codecov 顯示的專案覆蓋率</b>——
                Codecov 會額外套用 codecov.yml 的 <code>ignore:</code> 規則
                （Room *_Impl、ViewBinding、Hilt 生成碼等）。這裡的數字只用來
                在本地快速找出覆蓋率低的模組/檔案，正式的 70% 目標請以 Codecov PR 檢查為準。
              </div>
              <table>
                <thead><tr><th>Module</th><th>Line</th><th>Branch</th><th>Instruction</th><th>Progress</th></tr></thead>
                <tbody>$rows</tbody>
              </table>
              <p class="metadata">Generated by ./gradlew aggregateCoverageReport</p>
            </body>
            </html>
            """.trimIndent(),
        )
        logger.lifecycle("Coverage dashboard: ${output.resolve("index.html")}")
        logger.lifecycle("Raw project line coverage: ${pct(totalLines.percentage)} (need $linesNeededFor70 more lines for 70%)")
    }
}
