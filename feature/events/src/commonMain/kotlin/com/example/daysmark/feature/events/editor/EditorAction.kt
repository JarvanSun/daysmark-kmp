package com.example.daysmark.feature.events.editor

import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventIcon
import com.example.daysmark.core.domain.model.Reminder
import com.example.daysmark.core.domain.model.RepeatRule
import com.example.daysmark.core.domain.model.TimeOfDay

/** Everything a user can do in the editor. */
public sealed interface EditorAction {
    public data class SetTitle(val title: String) : EditorAction

    /** Moving the start past the end moves the end with it, keeping the range valid. */
    public data class SetDate(val date: CalendarDate) : EditorAction

    /** `null` makes the event all-day. */
    public data class SetTime(val time: TimeOfDay?) : EditorAction

    /** `null` makes it a single day; an end before the start is ignored. */
    public data class SetEndDate(val endDate: CalendarDate?) : EditorAction

    public data class SetRepeat(val repeat: RepeatRule) : EditorAction

    public data class SetCategory(val category: EventCategory) : EditorAction

    public data class SetIcon(val icon: EventIcon) : EditorAction

    /** Adds the first offset not yet used, at the morning time. */
    public data object AddReminder : EditorAction

    public data class UpdateReminder(val index: Int, val reminder: Reminder) : EditorAction

    public data class RemoveReminder(val index: Int) : EditorAction

    public data object Save : EditorAction

    /** The user saw [EditorUiState.Editing.failure]. */
    public data object DismissFailure : EditorAction

    /** Reads the event again after [EditorUiState.Failed]. */
    public data object Retry : EditorAction

    /** Sent after the user confirmed. */
    public data object Delete : EditorAction
}
