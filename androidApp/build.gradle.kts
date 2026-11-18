plugins {
    alias(libs.plugins.daysmark.android.application)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.daysmark.android"
    defaultConfig {
        // Distinct from the Swift Daysmark app so both can be installed side by side.
        applicationId = "com.example.daysmark.kmp"
        versionCode = 1
        versionName = "0.1.0"
    }
    // The artwork iOS also uses (WebP), from the one shared folder.
    sourceSets.named("main") { res.directories.add("../resources/artwork") }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
