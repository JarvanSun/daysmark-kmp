// The composition root: owns the Metro graph and is the one framework iOS links ("Shared").
plugins {
    alias(libs.plugins.daysmark.kmp.library)
    alias(libs.plugins.metro)
    alias(libs.plugins.kmp.nativecoroutines)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            // Only what Swift needs: ViewModels, their state, and domain values.
            export(projects.feature.events)
            export(projects.core.domain)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.feature.events)
            api(projects.core.domain)
            implementation(projects.core.data)
            implementation(libs.room.runtime)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.core.testing)
        }
    }
}

// The widget timeline format is shared with Swift through resources/fixtures.
tasks.named<Test>("jvmTest") {
    val fixtures = rootProject.layout.projectDirectory.dir("resources/fixtures")
    inputs.dir(fixtures)
    systemProperty("daysmark.fixtures", fixtures.asFile.path)
}

/**
 * The iOS framework's API is reviewed like code (docs/spec/ios-interop.md): every type it exports
 * is listed in api/framework-types.txt. `checkFrameworkApi` (part of `check`) fails when the header
 * differs from that list, so any new or removed type shows up in review; `updateFrameworkApi`
 * rewrites the list after an intended change. Storage and DI types may never be on it.
 */
val frameworkHeader =
    layout.buildDirectory.file(
        "bin/iosSimulatorArm64/debugFramework/Shared.framework/Headers/Shared.h"
    )
val frameworkBaseline = layout.projectDirectory.file("api/framework-types.txt")

val checkFrameworkApi by tasks.registering {
    group = "verification"
    description = "Fails if the Shared framework exports types not in api/framework-types.txt."
    dependsOn("linkDebugFrameworkIosSimulatorArm64")
    // Locals, not the script's vals: the configuration cache can't store the script.
    val header = frameworkHeader
    val baselineFile = frameworkBaseline
    inputs.file(header)
    inputs.file(baselineFile)
    doLast {
        val exported =
            Regex("""^@(?:interface|protocol) (Shared\w+)""", RegexOption.MULTILINE)
                .findAll(header.get().asFile.readText())
                .map { it.groupValues[1] }
                .toSortedSet()
                .toList()
        val forbidden = Regex("""(Room|Metro|Database|Dao|Entity|Sqlite|Driver|DataProviders)""")
        val leaks = exported.filter { forbidden.containsMatchIn(it) }
        check(leaks.isEmpty()) { "Storage or DI types reach Swift: $leaks" }
        val baseline = baselineFile.asFile.readLines().filter { it.isNotBlank() }
        val added = exported - baseline.toSet()
        val removed = baseline - exported.toSet()
        check(added.isEmpty() && removed.isEmpty()) {
            "The framework's API changed. Added: $added. Removed: $removed. " +
                "If intended, run ./gradlew :shared:updateFrameworkApi and commit the list."
        }
    }
}

val updateFrameworkApi by tasks.registering {
    group = "verification"
    description = "Rewrites api/framework-types.txt from the current framework header."
    dependsOn("linkDebugFrameworkIosSimulatorArm64")
    val header = frameworkHeader
    val baselineFile = frameworkBaseline
    inputs.file(header)
    outputs.file(baselineFile)
    doLast {
        val exported =
            Regex("""^@(?:interface|protocol) (Shared\w+)""", RegexOption.MULTILINE)
                .findAll(header.get().asFile.readText())
                .map { it.groupValues[1] }
                .toSortedSet()
        baselineFile.asFile.writeText(exported.joinToString("\n", postfix = "\n"))
    }
}

checkFrameworkApi { mustRunAfter(updateFrameworkApi) }

tasks.named("check") { dependsOn(checkFrameworkApi) }
