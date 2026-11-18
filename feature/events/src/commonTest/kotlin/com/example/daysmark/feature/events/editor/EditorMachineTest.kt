package com.example.daysmark.feature.events.editor

import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventIcon
import com.example.daysmark.core.domain.model.RepeatRule
import kotlin.test.Test
import kotlin.test.assertEquals

class EditorMachineTest {
    private val draft =
        EventDraft(
            title = "Trip",
            date = CalendarDate(2026, 12, 1),
            time = null,
            endDate = null,
            repeat = RepeatRule.NEVER,
            category = EventCategory.IMPORTANT,
            icon = EventIcon.AIRPLANE,
            reminders = emptyList(),
        )
    private val editing = EditorPhase.Editing(draft, draft, isNew = false)
    private val saving = EditorPhase.Saving(draft, draft, isNew = false)

    @Test
    fun whileAWriteRunsEditsAndASecondWriteAreIgnored() {
        assertEquals(saving, saving.on(EditorEvent.Edit(EditorAction.SetTitle("Other"))))
        assertEquals(saving, saving.on(EditorEvent.Save))
        assertEquals(saving, saving.on(EditorEvent.Delete))
    }

    @Test
    fun aWritesOutcomeEndsItOrReturnsToEditingWithTheFailure() {
        assertEquals(EditorPhase.Finished, saving.on(EditorEvent.WriteSucceeded))
        assertEquals(
            EditorPhase.Editing(draft, draft, isNew = false, EditorFailure.SAVING),
            saving.on(EditorEvent.WriteFailed),
        )
        assertEquals(
            EditorPhase.Editing(draft, draft, isNew = false, EditorFailure.DELETING),
            EditorPhase.Deleting(draft, draft).on(EditorEvent.WriteFailed),
        )
    }

    @Test
    fun theEventDisappearingEndsTheEditorFromAnyActivePhase() {
        listOf(EditorPhase.Loading, editing, saving, EditorPhase.Deleting(draft, draft)).forEach {
            assertEquals(EditorPhase.Finished, it.on(EditorEvent.Gone))
        }
    }

    @Test
    fun finishedIsFinal() {
        listOf(EditorEvent.Save, EditorEvent.Loaded(draft), EditorEvent.Retry).forEach {
            assertEquals(EditorPhase.Finished, EditorPhase.Finished.on(it))
        }
    }

    @Test
    fun aLaterCopyOfTheStoredEventDoesntOverwriteTheDraft() {
        val edited = editing.on(EditorEvent.Edit(EditorAction.SetTitle("Kyoto")))
        assertEquals(edited, edited.on(EditorEvent.Loaded(draft)))
    }

    @Test
    fun aBlankTitleCantBeSaved() {
        val blank = EditorPhase.Editing(draft.copy(title = "  "), draft, isNew = false)
        assertEquals(blank, blank.on(EditorEvent.Save))
    }

    @Test
    fun aFailedLoadCanBeRetried() {
        assertEquals(EditorPhase.LoadFailed, EditorPhase.Loading.on(EditorEvent.LoadFailed))
        assertEquals(EditorPhase.Loading, EditorPhase.LoadFailed.on(EditorEvent.Retry))
    }

    @Test
    fun aNewEventCantBeDeleted() {
        val new = EditorPhase.Editing(draft, draft, isNew = true)
        assertEquals(new, new.on(EditorEvent.Delete))
    }

    @Test
    fun retryIsAcceptedOnlyAfterAFailedLoad() {
        assertEquals(editing, editing.on(EditorEvent.Retry))
        assertEquals(EditorPhase.Finished, EditorPhase.Finished.on(EditorEvent.Retry))
    }

    @Test
    fun aRestoredSessionKeepsItsDraftAndWhatIsStored() {
        val edited = draft.copy(title = "Kyoto")
        assertEquals(
            EditorPhase.Editing(edited, draft, isNew = false),
            EditorPhase.Loading.on(EditorEvent.Loaded(edited, original = draft)),
        )
    }
}
