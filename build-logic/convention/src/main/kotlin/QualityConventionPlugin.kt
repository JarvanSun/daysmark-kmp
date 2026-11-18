import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/**
 * Formatting (ktfmt, kotlinlang style) and static analysis (detekt) for one module. Both are wired
 * into `check`, so `./gradlew check` is the single gate locally and in CI.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("com.diffplug.spotless")
            pluginManager.apply("dev.detekt")

            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("src/**/*.kt")
                    // Written by scripts/generate-design-tokens.py, which `--check`s it instead.
                    targetExclude("src/**/Generated*.kt")
                    ktfmt(libs.version("ktfmt")).kotlinlangStyle()
                }
                kotlinGradle {
                    target("*.gradle.kts")
                    ktfmt(libs.version("ktfmt")).kotlinlangStyle()
                }
            }

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig.set(true)
                allRules.set(false)
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                source.setFrom(fileTree("src") { include("**/*.kt"); exclude("**/Generated*.kt") })
            }
            tasks.withType<Detekt>().configureEach { jvmTarget.set(libs.version("jvm-target")) }

            tasks.named("check") { dependsOn("detekt", "spotlessCheck") }
        }
}
