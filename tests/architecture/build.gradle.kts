// Konsist tests that turn docs/spec rules into failing builds. Runs on the JVM, reads sources only.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.daysmark.quality)
}

kotlin {
    compilerOptions {
        jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(libs.versions.jvm.target.get())
        )
    }
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.target.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.target.get())
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}

// Konsist reads every module's sources, so any source change must rerun these tests.
tasks.test {
    inputs.files(
        rootProject.fileTree(".") {
            include("**/src/**/*.kt")
            exclude("**/build/**")
        }
    )
}
