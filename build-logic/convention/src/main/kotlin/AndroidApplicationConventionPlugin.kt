import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** The Android app module: AGP's built-in Kotlin, Compose, and the same SDK levels as the libraries. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("daysmark.quality")

            val javaVersion = JavaVersion.toVersion(libs.version("jvm-target"))
            extensions.configure<ApplicationExtension> {
                namespace = daysmarkNamespace
                compileSdk = libs.version("android-compileSdk").toInt()
                defaultConfig {
                    minSdk = libs.version("android-minSdk").toInt()
                    targetSdk = libs.version("android-targetSdk").toInt()
                }
                compileOptions {
                    sourceCompatibility = javaVersion
                    targetCompatibility = javaVersion
                }
                buildFeatures { compose = true }
                // Lint runs in `check`; a warning fails it like a Kotlin warning does.
                lint {
                    warningsAsErrors = true
                    abortOnError = true
                }
            }
            // The same bar as the shared modules: a deprecation or unchecked cast fails the build.
            tasks.withType<KotlinCompile>().configureEach {
                compilerOptions.allWarningsAsErrors.set(true)
            }
        }
}
