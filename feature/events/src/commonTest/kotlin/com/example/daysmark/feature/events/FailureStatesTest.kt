package com.example.daysmark.feature.events

import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.repository.StorageException
import com.example.daysmark.core.testing.FakeEventRepository
import com.example.daysmark.core.testing.FakeOnboardingRepository
import com.example.daysmark.core.testing.FixedWallClock
import com.example.daysmark.core.testing.RecordingErrorReporter
import com.example.daysmark.feature.events.detail.DetailAction
import com.example.daysmark.feature.events.detail.DetailUiState
import com.example.daysmark.feature.events.detail.DetailViewModel
import com.example.daysmark.feature.events.home.HomeAction
import com.example.daysmark.feature.events.home.HomeFailure
import com.example.daysmark.feature.events.home.HomeUiState
import com.example.daysmark.feature.events.home.HomeViewModel
import com.example.daysmark.feature.events.onboarding.LaunchAction
import com.example.daysmark.feature.events.onboarding.LaunchDestination
import com.example.daysmark.feature.events.onboarding.LaunchViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

/** Reads that fail become a state with Retry; writes that fail become a dismissible failure. */
@OptIn(ExperimentalCoroutinesApi::class)
class FailureStatesTest {
    private val reporter = RecordingErrorReporter()
    private val repository = FakeEventRepository()
    private val clock = FixedWallClock(LocalDate(2026, 11, 18))
    private val trip = Event(EventId("trip"), "Kyoto", CalendarDate(2026, 11, 25))

    private val owner = ViewModelOwner()

    @BeforeTest fun setUp() = owner.setUp()

    @AfterTest fun tearDown() = owner.tearDown()

    /** Collects [state] for the test, as a screen would. */
    private fun <T> TestScope.watch(state: StateFlow<T>): StateFlow<T> = state.also {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.collect {} }
    }

    @Test
    fun homeShowsAFailedReadAndRecoversOnRetry() = runTest {
        repository.failReads = true
        val home = owner.own(HomeViewModel(repository, clock, reporter))
        val state = watch(home.uiState)
        assertEquals(HomeUiState.Failed, state.value)

        repository.failReads = false
        home.onAction(HomeAction.Retry)
        assertTrue(state.value is HomeUiState.Content)
    }

    @Test
    fun aFailedPinOnHomeIsReportedUntilDismissed() = runTest {
        repository.events.value = listOf(trip)
        val home = owner.own(HomeViewModel(repository, clock, reporter))
        val state = watch(home.uiState)
        repository.writeFailure = StorageException("locked")

        home.onAction(HomeAction.TogglePin(trip.id))
        assertEquals(HomeFailure.PINNING, (state.value as HomeUiState.Content).failure)

        home.onAction(HomeAction.DismissFailure)
        assertEquals(null, (state.value as HomeUiState.Content).failure)
    }

    @Test
    fun aFailedDeleteOnHomeKeepsTheEvent() = runTest {
        repository.events.value = listOf(trip)
        val home = owner.own(HomeViewModel(repository, clock, reporter))
        val state = watch(home.uiState)
        repository.writeFailure = StorageException("locked")

        home.onAction(HomeAction.Delete(trip.id))

        assertEquals(HomeFailure.DELETING, (state.value as HomeUiState.Content).failure)
        assertEquals(listOf(trip), repository.events.value)
    }

    @Test
    fun detailShowsAFailedReadAndAFailedPin() = runTest {
        repository.events.value = listOf(trip)
        repository.failReads = true
        val detail = owner.own(DetailViewModel(trip.id, repository, clock, reporter))
        val state = watch(detail.uiState)
        assertEquals(DetailUiState.Failed, state.value)

        repository.failReads = false
        detail.onAction(DetailAction.Retry)
        repository.writeFailure = StorageException("locked")
        detail.onAction(DetailAction.TogglePin)
        assertTrue((state.value as DetailUiState.Content).pinFailed)
    }

    @Test
    fun theLaunchGateOffersRetryWhenTheFlagCantBeRead() = runTest {
        val onboarding = FakeOnboardingRepository().apply { failReads = true }
        val launch = owner.own(LaunchViewModel(onboarding, reporter))
        assertEquals(LaunchDestination.FAILED, launch.uiState.value)

        onboarding.failReads = false
        launch.onAction(LaunchAction.Retry)
        assertEquals(LaunchDestination.ONBOARDING, launch.uiState.value)
    }
}
