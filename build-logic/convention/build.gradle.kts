plugins { `kotlin-dsl` }

group = "daysmark.buildlogic"


dependencies {
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.detekt.gradle.plugin)
    compileOnly(libs.spotless.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = libs.plugins.daysmark.kmp.library.get().pluginId
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = libs.plugins.daysmark.android.application.get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("quality") {
            id = libs.plugins.daysmark.quality.get().pluginId
            implementationClass = "QualityConventionPlugin"
        }
    }
}
