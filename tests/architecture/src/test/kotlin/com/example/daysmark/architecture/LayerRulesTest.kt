package com.example.daysmark.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test
import kotlin.test.assertTrue as assertThat

/**
 * The layer rules of docs/spec/architecture.md, checked on every build. The Gradle module graph
 * already makes most wrong imports impossible; these tests catch what a module graph cannot see.
 */
class LayerRulesTest {
    private val production = Konsist.scopeFromProduction()

    private fun filesIn(modulePath: String): List<KoFileDeclaration> =
        production.files
            .filter { it.path.contains("/$modulePath/") }
            .also {
                // A rule over zero files would pass silently; fail instead when a module moves.
                assertThat(it.isNotEmpty(), "No production files found under $modulePath")
            }

    @Test
    fun domainUsesOnlyKotlinAndKotlinxLibraries() {
        val allowed =
            listOf(
                "kotlin.",
                "kotlinx.coroutines.",
                "kotlinx.datetime.",
                "com.example.daysmark.core.domain.",
            )
        filesIn("core/domain").assertTrue { file ->
            file.imports.all { import -> allowed.any { import.name.startsWith(it) } }
        }
    }

    @Test
    fun featuresNeverReachTheDataLayer() {
        filesIn("feature").assertFalse { file ->
            file.imports.any { it.name.startsWith("com.example.daysmark.core.data") }
        }
    }

    @Test
    fun dataLayerKeepsItsImplementationsInternal() {
        val files = filesIn("core/data")
        // Each kind is checked on its own, so a rule that matches nothing fails instead of passing.
        val repositories = files.flatMap { it.classes() }.filter { it.name.startsWith("Room") }
        val entities = files.flatMap { it.classes() }.filter { it.name.endsWith("Entity") }
        val daos = files.flatMap { it.interfaces() }.filter { it.name.endsWith("Dao") }
        listOf("repositories" to repositories, "entities" to entities, "DAOs" to daos).forEach {
            (kind, found) ->
            assertThat(found.isNotEmpty(), "No $kind found in core:data")
        }
        repositories.assertTrue { it.hasInternalModifier }
        entities.assertTrue { it.hasInternalModifier }
        daos.assertTrue { it.hasInternalModifier }
    }

    @Test
    fun repositoriesAreDomainInterfaces() {
        val repositories =
            production
                .interfaces()
                .filter { it.name.endsWith("Repository") }
                .also {
                    assertThat(it.isNotEmpty(), "No repository interfaces found")
                }
        repositories.assertTrue {
            it.resideInPackage("com.example.daysmark.core.domain.repository..")
        }
    }

    @Test
    fun productionCodeNeverUsesTestFakes() {
        production.files
            .filterNot { it.path.contains("/core/testing/") }
            .assertFalse { file ->
                file.imports.any { it.name.startsWith("com.example.daysmark.core.testing") }
            }
    }

    /** detekt's rule for this needs type resolution, so the check lives here. */
    @Test
    fun productionCodeNeverAssertsNonNull() {
        production.files.assertFalse { file -> "!!" in file.text }
    }
}
