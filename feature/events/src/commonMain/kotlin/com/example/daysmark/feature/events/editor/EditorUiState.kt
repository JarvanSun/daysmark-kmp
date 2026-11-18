package com.example.daysmark.feature.events.editor

import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.calculation.RangePhase
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventIcon
import com.example.daysmark.core.domain.model.Reminder
import com.example.daysmark.core.domain.model.RepeatRule
import com.example.daysmark.core.domain.model.TimeOfDay

/** Everything the editor shows. One sheet both creates and edits an event. */
public sealed interface EditorUiState {
    public data object Loading : EditorUiState

    /** The event couldn't be read; the platform offers [EditorAction.Retry]. */
    public data object Failed : EditorUiState

    /** Saved, deleted, or deleted elsewhere; the platform closes the editor. */
    public data object Finished : EditorUiState

    public data class Editing(
        /** What the platform saves to resume this editor after the process is gone. */
        val session: EditorSession,
        val draft: EventDraft,
        val isNew: Boolean,
        /** A title and a valid date are all a new event needs; not while a write is running. */
        val canSave: Boolean,
        /** A save or delete is running; the platform disables Save and Delete. */
        val isBusy: Boolean,
        /** The last write couldn't be stored; cleared by [EditorAction.DismissFailure]. */
        val failure: EditorFailure?,
        /** Unsaved changes: the sheet can't be swiped away. */
        val hasChanges: Boolean,
        /** The live preview row: how the draft would count right now. */
        val preview: EditorPreview,
        /** Days-before choices for a reminder, "on the day" first. */
        val reminderChoices: List<Int>,
    ) : EditorUiState
}

/** The editable fields. Never stored as is: [EditorViewModel] turns it into an event on save. */
public data class EventDraft(
    val title: String,
    val date: CalendarDate,
    val time: TimeOfDay?,
    val endDate: CalendarDate?,
    val repeat: RepeatRule,
    val category: EventCategory,
    val icon: EventIcon,
    val reminders: List<Reminder>,
)

public data class EditorPreview(val dayCount: DayCount, val phase: RangePhase)

/** Which write couldn't be stored. */
public enum class EditorFailure {
    SAVING,
    DELETING,
}
