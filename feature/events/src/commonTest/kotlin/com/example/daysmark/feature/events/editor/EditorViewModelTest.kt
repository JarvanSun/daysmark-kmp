package com.example.daysmark.feature.events.editor

import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.calculation.RangePhase
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventIcon
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.Reminder
import com.example.daysmark.core.domain.model.RepeatRule
import com.example.daysmark.core.domain.model.TimeOfDay
import com.example.daysmark.core.domain.repository.StorageException
import com.example.daysmark.core.testing.FakeEventRepository
import com.example.daysmark.core.testing.FixedWallClock
import com.example.daysmark.core.testing.RecordingErrorReporter
import com.example.daysmark.feature.events.ViewModelOwner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {
    private val reporter = RecordingErrorReporter()
    private val repository = FakeEventRepository()
    private val clock = FixedWallClock(LocalDate(2026, 11, 18))

    private val owner = ViewModelOwner()

    @BeforeTest fun setUp() = owner.setUp()

    @AfterTest fun tearDown() = owner.tearDown()

    private fun newEditor(category: EventCategory = EventCategory.IMPORTANT) =
        owner.own(EditorViewModel(EditorTarget.New(category), repository, clock, reporter))

    private fun editing(viewModel: EditorViewModel) =
        viewModel.uiState.value as EditorUiState.Editing

    private val trip =
        Event(
            EventId("trip"),
            "Kyoto",
            CalendarDate(2026, 12, 1),
            endDate = CalendarDate(2026, 12, 5),
            icon = EventIcon.AIRPLANE,
            reminders = listOf(Reminder(1, TimeOfDay.MORNING)),
        )

    @Test
    fun aNewEventStartsTodayWithoutATitleAndCannotBeSavedYet() {
        val state = editing(newEditor())
        assertTrue(state.isNew)
        assertEquals(CalendarDate(2026, 11, 18), state.draft.date)
        assertFalse(state.canSave)
        assertEquals(DayCount.Today, state.preview.dayCount)
    }

    @Test
    fun birthdaysAndAnniversariesStartYearly() {
        assertEquals(RepeatRule.YEARLY, editing(newEditor(EventCategory.BIRTHDAY)).draft.repeat)
        assertEquals(RepeatRule.NEVER, editing(newEditor()).draft.repeat)
    }

    @Test
    fun theLivePreviewFollowsTheDraft() {
        val editor = newEditor()
        editor.onAction(EditorAction.SetDate(CalendarDate(2026, 11, 30)))
        assertEquals(DayCount.Future(12), editing(editor).preview.dayCount)

        editor.onAction(EditorAction.SetEndDate(CalendarDate(2026, 12, 2)))
        assertEquals(RangePhase.BEFORE_START, editing(editor).preview.phase)
    }

    @Test
    fun savingStoresATrimmedTitleAndFinishes() = runTest {
        val editor = newEditor(EventCategory.BIRTHDAY)
        editor.onAction(EditorAction.SetTitle("  Mia  "))
        assertTrue(editing(editor).canSave)

        editor.onAction(EditorAction.Save)

        val saved = repository.events.value.single()
        assertEquals("Mia", saved.title)
        assertEquals(EventCategory.BIRTHDAY, saved.category)
        assertEquals(EditorUiState.Finished, editor.uiState.value)
    }

    @Test
    fun editingLoadsTheEventAndTracksChanges() = runTest {
        repository.events.value = listOf(trip)
        val editor =
            owner.own(EditorViewModel(EditorTarget.Existing(trip.id), repository, clock, reporter))

        assertFalse(editing(editor).isNew)
        assertFalse(editing(editor).hasChanges)
        editor.onAction(EditorAction.SetTitle("Kyoto in autumn"))
        assertTrue(editing(editor).hasChanges)

        editor.onAction(EditorAction.Save)
        assertEquals("Kyoto in autumn", repository.events.value.single().title)
        assertEquals(trip.reminders, repository.events.value.single().reminders)
    }

    @Test
    fun deleteRemovesTheEventAndFinishes() = runTest {
        repository.events.value = listOf(trip)
        val editor =
            owner.own(EditorViewModel(EditorTarget.Existing(trip.id), repository, clock, reporter))

        editor.onAction(EditorAction.Delete)

        assertEquals(emptyList(), repository.events.value)
        assertEquals(EditorUiState.Finished, editor.uiState.value)
    }

    @Test
    fun anEventThatNoLongerExistsFinishesTheEditor() = runTest {
        val editor =
            owner.own(
                EditorViewModel(EditorTarget.Existing(EventId("gone")), repository, clock, reporter)
            )
        assertEquals(EditorUiState.Finished, editor.uiState.value)
    }

    @Test
    fun aSaveThatFindsTheEventDeletedClosesTheEditorWithoutRecreatingIt() = runTest {
        repository.events.value = listOf(trip)
        val gate = CompletableDeferred<Unit>()
        val editor =
            owner.own(EditorViewModel(EditorTarget.Existing(trip.id), repository, clock, reporter))
        editor.onAction(EditorAction.SetTitle("Kyoto in autumn"))
        repository.writeGate = gate
        editor.onAction(EditorAction.Save)

        // Deleted on another screen while the save waits.
        repository.writeGate = null
        repository.delete(trip.id)
        gate.complete(Unit)

        assertEquals(EditorUiState.Finished, editor.uiState.value)
        assertEquals(emptyList(), repository.events.value)
    }

    @Test
    fun anUnreadableEventCanBeReadAgain() = runTest {
        repository.events.value = listOf(trip)
        repository.failReads = true
        val editor =
            owner.own(EditorViewModel(EditorTarget.Existing(trip.id), repository, clock, reporter))
        assertEquals(EditorUiState.Failed, editor.uiState.value)

        repository.failReads = false
        editor.onAction(EditorAction.Retry)
        assertEquals("Kyoto", editing(editor).draft.title)
    }

    @Test
    fun aFailedDeleteKeepsEditingAndSaysSo() = runTest {
        repository.events.value = listOf(trip)
        val editor =
            owner.own(EditorViewModel(EditorTarget.Existing(trip.id), repository, clock, reporter))
        repository.writeFailure = StorageException("locked")

        editor.onAction(EditorAction.Delete)

        assertEquals(EditorFailure.DELETING, editing(editor).failure)
        assertEquals(listOf(trip), repository.events.value)
    }

    @Test
    fun anEventDeletedElsewhereWhileEditingClosesTheEditorInsteadOfBeingSavedBack() = runTest {
        repository.events.value = listOf(trip)
        val editor =
            owner.own(EditorViewModel(EditorTarget.Existing(trip.id), repository, clock, reporter))
        editor.onAction(EditorAction.SetTitle("Kyoto in autumn"))

        repository.events.value = emptyList()

        assertEquals(EditorUiState.Finished, editor.uiState.value)
    }

    @Test
    fun tappingSaveTwiceSavesOnce() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.writeGate = gate
        val editor = newEditor()
        editor.onAction(EditorAction.SetTitle("Trip"))

        editor.onAction(EditorAction.Save)
        assertFalse(editing(editor).canSave)
        editor.onAction(EditorAction.Save)
        gate.complete(Unit)

        assertEquals(1, repository.writeCount)
        assertEquals(1, repository.events.value.size)
    }

    @Test
    fun aFailedSaveKeepsTheDraftAndCanBeRetriedAsTheSameEvent() = runTest {
        repository.writeFailure = StorageException("disk full")
        val editor = newEditor()
        editor.onAction(EditorAction.SetTitle("Trip"))

        editor.onAction(EditorAction.Save)
        assertTrue((editing(editor).failure == EditorFailure.SAVING))
        assertEquals("Trip", editing(editor).draft.title)

        editor.onAction(EditorAction.DismissFailure)
        editor.onAction(EditorAction.Save)
        editor.onAction(EditorAction.Save)
        assertEquals(EditorUiState.Finished, editor.uiState.value)
        assertEquals(1, repository.events.value.size)
    }
}

class DraftEditingTest {
    private val draft =
        EventDraft(
            title = "Trip",
            date = CalendarDate(2026, 12, 1),
            time = null,
            endDate = CalendarDate(2026, 12, 5),
            repeat = RepeatRule.NEVER,
            category = EventCategory.IMPORTANT,
            icon = EventIcon.CALENDAR,
            reminders = emptyList(),
        )

    @Test
    fun movingTheStartPastTheEndMovesTheEndWithIt() {
        val moved = DraftEditing.apply(draft, EditorAction.SetDate(CalendarDate(2026, 12, 9)))
        assertEquals(CalendarDate(2026, 12, 9), moved.endDate)
    }

    @Test
    fun anEndBeforeTheStartIsIgnored() {
        assertEquals(
            draft,
            DraftEditing.apply(draft, EditorAction.SetEndDate(CalendarDate(2026, 11, 1))),
        )
    }

    @Test
    fun remindersAddTheNextUnusedOffsetAtTheLastTime() {
        var edited = DraftEditing.apply(draft, EditorAction.AddReminder)
        assertEquals(Reminder(0, TimeOfDay.MORNING), edited.reminders.single())

        edited =
            DraftEditing.apply(
                edited,
                EditorAction.UpdateReminder(0, Reminder(0, TimeOfDay(20, 0))),
            )
        edited = DraftEditing.apply(edited, EditorAction.AddReminder)
        assertEquals(Reminder(1, TimeOfDay(20, 0)), edited.reminders[1])

        edited = DraftEditing.apply(edited, EditorAction.RemoveReminder(0))
        assertEquals(listOf(Reminder(1, TimeOfDay(20, 0))), edited.reminders)
    }
}
