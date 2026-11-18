plugins {
    alias(libs.plugins.daysmark.kmp.library)
    alias(libs.plugins.metro)
    alias(libs.plugins.kmp.nativecoroutines)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // A feature sees the domain only: never :core:data (docs/spec/architecture.md).
            api(projects.core.domain)
            api(libs.kmp.observableviewmodel.core)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
