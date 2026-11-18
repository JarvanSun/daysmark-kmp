plugins {
    alias(libs.plugins.daysmark.kmp.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.core.testing)
        }
    }
}

room { schemaDirectory("$projectDir/schemas") }

dependencies {
    listOf("kspAndroid", "kspJvm", "kspIosArm64", "kspIosSimulatorArm64").forEach {
        add(it, libs.room.compiler)
    }
}

// Android Lint reads every source set's generated code, including Room's KSP output for the other
// targets, but doesn't declare that it depends on it. Without this, lint races KSP and fails.
tasks
    .matching { it.name.startsWith("lint") }
    .configureEach {
        dependsOn(tasks.matching { it.name.startsWith("ksp") && it.name.contains("Kotlin") })
    }
