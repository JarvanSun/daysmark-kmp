// Pure Kotlin: only kotlinx libraries, no framework, no DI annotations (docs/spec/architecture.md).
plugins { alias(libs.plugins.daysmark.kmp.library) }

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.core.testing)
        }
    }
}
