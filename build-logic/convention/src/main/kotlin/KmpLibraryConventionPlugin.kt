import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

/**
 * Every shared module: Android, iOS (device + Apple-silicon simulator) and a JVM target that only
 * exists so `jvmTest` gives fast feedback. Explicit API mode and warnings-as-errors are not
 * optional here, so no module can opt out (docs/spec/quality.md).
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            pluginManager.apply("daysmark.quality")

            val jvmTarget = JvmTarget.fromTarget(libs.version("jvm-target"))

            extensions.configure<KotlinMultiplatformExtension> {
                explicitApi()

                targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                    namespace = daysmarkNamespace
                    compileSdk = libs.version("android-compileSdk").toInt()
                    minSdk = libs.version("android-minSdk").toInt()
                    compilerOptions { this.jvmTarget.set(jvmTarget) }
                    // commonTest also runs against the Android variants (e.g. androidx ViewModel).
                    withHostTest {}
                }
                jvm { compilerOptions { this.jvmTarget.set(jvmTarget) } }
                iosArm64()
                iosSimulatorArm64()

                compilerOptions {
                    allWarningsAsErrors.set(true)
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
                sourceSets.configureEach {
                    languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
                }
            }

            tasks.withType<JavaCompile>().configureEach {
                options.release.set(libs.version("jvm-target").toInt())
            }
            tasks.withType<KotlinNativeSimulatorTest>().configureEach {
                device.set(providers.gradleProperty("daysmark.iosTestDevice"))
            }
        }
}
