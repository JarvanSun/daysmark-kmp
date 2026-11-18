package com.example.daysmark.feature.events.onboarding

import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.onboarding.StarterEvent
import com.example.daysmark.core.domain.repository.StorageException
import com.example.daysmark.core.testing.FakeOnboardingRepository
import com.example.daysmark.core.testing.FixedWallClock
import com.example.daysmark.core.testing.RecordingErrorReporter
import com.example.daysmark.feature.events.ViewModelOwner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val reporter = RecordingErrorReporter()
    private val repository = FakeOnboardingRepository()
    private val clock = FixedWallClock(LocalDate(2026, 11, 18))
    private val titles = StarterEvent.entries.associateWith { it.name }

    private val owner = ViewModelOwner()

    @BeforeTest fun setUp() = owner.setUp()

    @AfterTest fun tearDown() = owner.tearDown()

    /** A ViewModel whose state is being collected, as a screen would. */
    private fun TestScope.viewModel() =
        owner.own(OnboardingViewModel(repository, clock, reporter)).also { model ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                model.uiState.collect {}
            }
        }

    private val OnboardingViewModel.state
        get() = uiState.value

    @Test
    fun welcomeCountsDownToNewYear() = runTest {
        val state = viewModel().state
        assertEquals(OnboardingStep.WELCOME, state.step)
        assertEquals(StarterCount(StarterEvent.NEXT_NEW_YEAR, DayCount.Future(44)), state.headline)
        assertFalse(state.canGoBack)
    }

    @Test
    fun theBirthdayStepCountsToTheNextBirthdayAndSinceTheBirthDate() = runTest {
        val model = viewModel()
        model.onAction(OnboardingAction.Continue)
        model.onAction(OnboardingAction.SetBirthday(CalendarDate(1996, 11, 20)))

        assertEquals(DayCount.Future(2), model.state.headline?.dayCount)
        assertEquals(StarterEvent.BIRTH_DATE, model.state.detail?.starter)
        assertTrue(model.state.detail?.dayCount is DayCount.Past)
        assertFalse(model.state.celebratesBirthday)
    }

    @Test
    fun aBirthdayTodayIsCelebrated() = runTest {
        val model = viewModel()
        model.onAction(OnboardingAction.Continue)
        model.onAction(OnboardingAction.SetBirthday(CalendarDate(1996, 11, 18)))

        assertTrue(model.state.celebratesBirthday)
    }

    @Test
    fun continuingNeedsAPickedBirthdaySoTheStartingDateIsNeverSavedByAccident() = runTest {
        val model = viewModel()
        model.onAction(OnboardingAction.Continue)
        assertFalse(model.state.canContinue)
        assertEquals(true, model.state.headline?.isPending)

        model.onAction(OnboardingAction.Continue)
        assertEquals(OnboardingStep.BIRTHDAY, model.state.step)

        model.onAction(OnboardingAction.SetBirthday(CalendarDate(1996, 3, 3)))
        assertTrue(model.state.canContinue)
    }

    @Test
    fun keepingTheBirthdayOffersItsStartersAllSelected() = runTest {
        val model = viewModel()
        model.onAction(OnboardingAction.Continue)
        model.onAction(OnboardingAction.SetBirthday(CalendarDate(1996, 3, 3)))
        model.onAction(OnboardingAction.Continue)

        assertEquals(StarterEvent.entries, model.state.choices.map { it.starter })
        assertTrue(model.state.choices.all { it.isSelected })
        assertEquals(OnboardingPrimaryAction.CREATE_SELECTED, model.state.primaryAction)
    }

    @Test
    fun skippingTheBirthdayLeavesOnlyTheGeneralStarters() = runTest {
        val model = viewModel()
        model.onAction(OnboardingAction.Continue)
        model.onAction(OnboardingAction.SkipBirthday)

        assertEquals(
            listOf(StarterEvent.NEXT_NEW_YEAR, StarterEvent.NEXT_MONTH),
            model.state.choices.map { it.starter },
        )
    }

    @Test
    fun deselectingEverythingTurnsTheButtonIntoStartEmpty() = runTest {
        val model = viewModel()
        model.onAction(OnboardingAction.Continue)
        model.onAction(OnboardingAction.SkipBirthday)
        model.onAction(OnboardingAction.Toggle(StarterEvent.NEXT_NEW_YEAR))
        model.onAction(OnboardingAction.Toggle(StarterEvent.NEXT_MONTH))

        assertEquals(OnboardingPrimaryAction.START_EMPTY, model.state.primaryAction)
    }

    @Test
    fun finishingSavesOnlyTheSelectedStartersWithTheirTitles() = runTest {
        val model = viewModel()
        model.onAction(OnboardingAction.Continue)
        model.onAction(OnboardingAction.SetBirthday(CalendarDate(1996, 3, 3)))
        model.onAction(OnboardingAction.Continue)
        model.onAction(OnboardingAction.Toggle(StarterEvent.NEXT_MONTH))
        model.onAction(OnboardingAction.Finish(titles))

        assertTrue(repository.isCompleted.value)
        assertEquals(
            listOf("BIRTHDAY", "BIRTH_DATE", "NEXT_NEW_YEAR"),
            repository.savedEvents.map { it.title },
        )
    }

    @Test
    fun skippingOnTheWelcomeStepFinishesWithNoEvents() = runTest {
        viewModel().onAction(OnboardingAction.StartEmpty)

        assertTrue(repository.isCompleted.value)
        assertEquals(emptyList(), repository.savedEvents)
    }

    @Test
    fun aFailedSaveCanBeRetried() = runTest {
        repository.failure = StorageException("disk full")
        val model = viewModel()
        model.onAction(OnboardingAction.StartEmpty)

        assertTrue(model.state.saveFailed)
        assertFalse(model.state.isSaving)
        assertFalse(repository.isCompleted.value)

        model.onAction(OnboardingAction.RetrySave)
        assertTrue(repository.isCompleted.value)
    }

    @Test
    fun retryingAFailedSkipStillCreatesNothing() = runTest {
        repository.failure = StorageException("disk full")
        val model = viewModel()
        model.onAction(OnboardingAction.StartEmpty)
        model.onAction(OnboardingAction.RetrySave)

        assertTrue(repository.isCompleted.value)
        assertEquals(emptyList(), repository.savedEvents)
    }

    @Test
    fun onlyFinishingWithTheBirthdayAsksForNotifications() = runTest {
        val model = viewModel()
        model.onAction(OnboardingAction.Continue)
        model.onAction(OnboardingAction.SetBirthday(CalendarDate(1996, 3, 3)))
        model.onAction(OnboardingAction.Continue)
        assertTrue(model.state.finishAsksForNotifications)

        model.onAction(OnboardingAction.Toggle(StarterEvent.BIRTHDAY))
        assertFalse(model.state.finishAsksForNotifications)
    }
}
