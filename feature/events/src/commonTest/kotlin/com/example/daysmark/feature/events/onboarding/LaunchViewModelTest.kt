package com.example.daysmark.feature.events.onboarding

import com.example.daysmark.core.testing.FakeOnboardingRepository
import com.example.daysmark.core.testing.RecordingErrorReporter
import com.example.daysmark.feature.events.ViewModelOwner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class LaunchViewModelTest {
    private val reporter = RecordingErrorReporter()
    private val repository = FakeOnboardingRepository()

    private val owner = ViewModelOwner()

    @BeforeTest fun setUp() = owner.setUp()

    @AfterTest fun tearDown() = owner.tearDown()

    @Test
    fun firstRunOpensOnboardingThenHomeOnceItIsDone() {
        val model = owner.own(LaunchViewModel(repository, reporter))
        assertEquals(LaunchDestination.ONBOARDING, model.uiState.value)

        repository.isCompleted.value = true
        assertEquals(LaunchDestination.HOME, model.uiState.value)
    }
}
