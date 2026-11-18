package com.example.daysmark.feature.events

import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.testing.FakeEventRepository
import com.example.daysmark.core.testing.FixedWallClock
import com.example.daysmark.core.testing.RecordingErrorReporter
import com.example.daysmark.feature.events.detail.DetailAction
import com.example.daysmark.feature.events.detail.DetailUiState
import com.example.daysmark.feature.events.detail.DetailUnit
import com.example.daysmark.feature.events.detail.DetailViewModel
import com.example.daysmark.feature.events.editor.EditorAction
import com.example.daysmark.feature.events.editor.EditorTarget
import com.example.daysmark.feature.events.editor.EditorUiState
import com.example.daysmark.feature.events.editor.EditorViewModel
import com.example.daysmark.feature.events.home.HomeUiState
import com.example.daysmark.feature.events.home.HomeViewModel
import com.rickclephas.kmp.observableviewmodel.InternalKMPObservableViewModelApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

/**
 * Ordering and lifetime, on a dispatcher that runs nothing until the test advances it: actions sent
 * back to back, subscriptions ending and starting again, a ViewModel being cleared.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LifecycleTest {
    private val reporter = RecordingErrorReporter()
    private val owner = ViewModelOwner(immediate = false)
    private val repository = FakeEventRepository()
    private val clock = FixedWallClock(LocalDate(2026, 11, 18))
    private val trip = Event(EventId("trip"), "Kyoto", CalendarDate(2026, 12, 25))

    @BeforeTest fun setUp() = owner.setUp()

    @AfterTest fun tearDown() = owner.tearDown()

    @Test
    fun twoQuickUnitTapsAreTwoSteps() = runTest {
        repository.events.value = listOf(trip)
        val detail = owner.own(DetailViewModel(trip.id, repository, clock, reporter))
        val collector = backgroundScope.launch { detail.uiState.collect {} }
        runCurrent()

        detail.onAction(DetailAction.CycleUnit)
        detail.onAction(DetailAction.CycleUnit)
        runCurrent()

        assertEquals(DetailUnit.HOURS, (detail.uiState.value as DetailUiState.Content).count.unit)
        collector.cancel()
    }

    @Test
    fun aDeleteSentWhileASaveIsPendingIsIgnored() = runTest {
        repository.events.value = listOf(trip)
        val editor =
            owner.own(EditorViewModel(EditorTarget.Existing(trip.id), repository, clock, reporter))
        runCurrent()

        editor.onAction(EditorAction.SetTitle("Kyoto in winter"))
        editor.onAction(EditorAction.Save)
        editor.onAction(EditorAction.Delete)
        advanceUntilIdle()

        assertEquals(EditorUiState.Finished, editor.uiState.value)
        assertEquals(listOf("Kyoto in winter"), repository.events.value.map { it.title })
        assertEquals(1, repository.writeCount)
    }

    @Test
    fun homeStopsReadingShortlyAfterTheScreenLeavesAndResumesWhenItReturns() = runTest {
        val home = owner.own(HomeViewModel(repository, clock, reporter))
        val first = backgroundScope.launch { home.uiState.collect {} }
        runCurrent()
        assertTrue(repository.activeReaders > 0)

        first.cancel()
        advanceTimeBy(6.seconds)
        runCurrent()
        assertEquals(0, repository.activeReaders)

        repository.events.value = listOf(trip)
        val second = backgroundScope.launch { home.uiState.collect {} }
        runCurrent()
        assertEquals(
            listOf(trip.id),
            (home.uiState.value as HomeUiState.Content).rows.map { it.id },
        )
        second.cancel()
    }

    @OptIn(InternalKMPObservableViewModelApi::class) // clear(): see ViewModelOwner.tearDown
    @Test
    fun clearingTheEditorStopsItsWatch() = runTest {
        repository.events.value = listOf(trip)
        val editor = EditorViewModel(EditorTarget.Existing(trip.id), repository, clock, reporter)
        runCurrent()
        assertTrue(repository.activeReaders > 0)

        editor.clear()
        runCurrent()

        assertEquals(0, repository.activeReaders)
    }

    @Test
    fun aWatchThatFailsWhileEditingReconnectsAndStillSeesADeletion() = runTest {
        repository.events.value = listOf(trip)
        val editor =
            owner.own(EditorViewModel(EditorTarget.Existing(trip.id), repository, clock, reporter))
        runCurrent()
        editor.onAction(EditorAction.SetTitle("Kyoto in winter"))
        runCurrent()

        repository.failReads = true // the database fails while the editor is open
        runCurrent()
        assertEquals("Kyoto in winter", (editor.uiState.value as EditorUiState.Editing).draft.title)
        assertTrue(reporter.reported.isNotEmpty())

        repository.failReads = false
        advanceTimeBy(2.seconds) // the reconnect pause
        runCurrent()
        repository.delete(trip.id)
        runCurrent()

        assertEquals(EditorUiState.Finished, editor.uiState.value)
    }

    @Test
    fun retryOnlyReadsAgainAfterAFailedLoad() = runTest {
        repository.events.value = listOf(trip)
        val editor =
            owner.own(EditorViewModel(EditorTarget.Existing(trip.id), repository, clock, reporter))
        runCurrent()
        val readersWhileEditing = repository.activeReaders

        editor.onAction(EditorAction.Retry)
        runCurrent()

        assertEquals(readersWhileEditing, repository.activeReaders)
        assertTrue(editor.uiState.value is EditorUiState.Editing)
    }

    @Test
    fun aRestoredSessionResumesTheDraftAndStillWatchesTheEvent() = runTest {
        repository.events.value = listOf(trip)
        val first =
            owner.own(EditorViewModel(EditorTarget.Existing(trip.id), repository, clock, reporter))
        runCurrent()
        first.onAction(EditorAction.SetTitle("Kyoto in winter"))
        runCurrent()
        val session = (first.uiState.value as EditorUiState.Editing).session

        // The process is gone; the platform gives the saved session to a new editor.
        val resumed =
            owner.own(EditorViewModel(EditorTarget.Resume(session), repository, clock, reporter))
        runCurrent()
        val state = resumed.uiState.value as EditorUiState.Editing
        assertEquals("Kyoto in winter", state.draft.title)
        assertTrue(state.hasChanges)

        repository.delete(trip.id)
        runCurrent()
        assertEquals(EditorUiState.Finished, resumed.uiState.value)
    }
}
