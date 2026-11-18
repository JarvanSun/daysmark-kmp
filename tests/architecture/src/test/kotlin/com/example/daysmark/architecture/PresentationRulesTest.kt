package com.example.daysmark.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test
import kotlin.test.assertTrue as assertThat

/** The unidirectional-data-flow contract of docs/spec/presentation.md. */
class PresentationRulesTest {
    private val viewModels =
        Konsist.scopeFromProduction()
            .classes()
            .filter { it.name.endsWith("ViewModel") }
            .also {
                assertThat(it.isNotEmpty(), "No ViewModels found")
            }

    @Test
    fun viewModelsLiveInFeatureModules() {
        viewModels.assertTrue { it.resideInPackage("com.example.daysmark.feature..") }
    }

    @Test
    fun viewModelsExposeOneStateAndOneInput() {
        viewModels.assertTrue { viewModel ->
            val publicFunctions =
                viewModel.functions(includeNested = false, includeLocal = false).filter {
                    it.hasPublicOrDefaultModifier
                }
            val publicProperties =
                viewModel.properties(includeNested = false).filter { it.hasPublicOrDefaultModifier }
            publicFunctions.map { it.name } == listOf("onAction") &&
                publicProperties.map { it.name } == listOf("uiState")
        }
    }

    /** `val uiState: StateFlow<…UiState or …Destination>`: read-only, never a mutable flow. */
    @Test
    fun theStateIsAReadOnlyStateFlow() {
        viewModels.assertTrue { viewModel ->
            val state = viewModel.properties(includeNested = false).single { it.name == "uiState" }
            state.isVal && state.type?.text?.startsWith("StateFlow<") == true
        }
    }

    /** `fun onAction(action: …Action)` returning nothing: the one typed input. */
    @Test
    fun theInputTakesOneTypedAction() {
        viewModels.assertTrue { viewModel ->
            val input = viewModel.functions(includeNested = false).single { it.name == "onAction" }
            val parameter = input.parameters.singleOrNull()
            parameter?.type?.text?.endsWith("Action") == true &&
                (input.returnType == null || input.returnType?.text == "Unit")
        }
    }

    /** States and their parts are values: every property of a feature's data class is a `val`. */
    @Test
    fun featureDataClassesAreImmutable() {
        val values =
            Konsist.scopeFromProduction().classes(includeNested = true).filter {
                it.hasDataModifier && it.resideInPackage("com.example.daysmark.feature..")
            }
        assertThat(values.isNotEmpty(), "No data classes found in feature modules")
        values.assertTrue { value ->
            value.primaryConstructor?.parameters.orEmpty().all { it.hasValModifier } &&
                value.properties(includeNested = false).none { it.isVar } &&
                value.primaryConstructor?.parameters.orEmpty().none {
                    it.type.text.startsWith("Mutable")
                }
        }
    }

    @Test
    fun uiStatesAndActionsAreSealed() {
        val contracts =
            Konsist.scopeFromProduction().interfaces().filter {
                it.name.endsWith("UiState") || it.name.endsWith("Action")
            }
        assertThat(contracts.isNotEmpty(), "No UiState or Action types found")
        contracts.assertTrue { it.hasSealedModifier }
    }

    /**
     * Swift sees `NoMatches` as `nomatches` but `NO_MATCHES` as `noMatches`, so public enums in the
     * modules exported to iOS name their entries in UPPER_SNAKE_CASE (docs/spec/ios-interop.md).
     */
    @Test
    fun exportedEnumEntriesAreUpperSnakeCase() {
        val exported =
            Konsist.scopeFromProduction()
                .classes()
                .filter { it.hasEnumModifier && it.hasPublicOrDefaultModifier }
                .filter {
                    it.resideInPackage("com.example.daysmark.core.domain..") ||
                        it.resideInPackage("com.example.daysmark.feature..")
                }
        assertThat(exported.isNotEmpty(), "No exported enums found")
        exported.assertTrue { enum ->
            enum.enumConstants.all { it.name.matches(Regex("[A-Z][A-Z0-9_]*")) }
        }
    }
}
