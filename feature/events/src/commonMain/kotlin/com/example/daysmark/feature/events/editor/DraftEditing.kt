package com.example.daysmark.feature.events.editor

import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Reminder
import com.example.daysmark.core.domain.model.TimeOfDay

/** The editing rules, as a pure function of draft and action, so they are tested directly. */
internal object DraftEditing {
    /** On the day, then 1, 2, 3 days, a week, two weeks and a month before. */
    val REMINDER_DAYS: List<Int> = listOf(0, 1, 2, 3, 7, 14, 30)

    fun apply(draft: EventDraft, action: EditorAction): EventDraft =
        when (action) {
            is EditorAction.SetTitle -> draft.copy(title = action.title)
            is EditorAction.SetDate -> draft.withStart(action.date)
            is EditorAction.SetTime -> draft.copy(time = action.time)
            is EditorAction.SetEndDate -> draft.withEnd(action.endDate)
            is EditorAction.SetRepeat -> draft.copy(repeat = action.repeat)
            is EditorAction.SetCategory -> draft.copy(category = action.category)
            is EditorAction.SetIcon -> draft.copy(icon = action.icon)
            EditorAction.AddReminder ->
                draft.copy(reminders = draft.reminders + nextReminder(draft))
            is EditorAction.UpdateReminder ->
                draft.copy(
                    reminders =
                        draft.reminders.mapIndexed { i, r ->
                            if (i == action.index) action.reminder else r
                        }
                )
            is EditorAction.RemoveReminder ->
                draft.copy(reminders = draft.reminders.filterIndexed { i, _ -> i != action.index })
            EditorAction.Save,
            EditorAction.Delete,
            EditorAction.DismissFailure,
            EditorAction.Retry -> draft
        }

    private fun EventDraft.withStart(start: CalendarDate): EventDraft {
        val end = endDate ?: return copy(date = start)
        return copy(
            date = start,
            endDate = if (end.toLocalDate() < start.toLocalDate()) start else end,
        )
    }

    private fun EventDraft.withEnd(end: CalendarDate?): EventDraft =
        if (end != null && end.toLocalDate() < date.toLocalDate()) this else copy(endDate = end)

    private fun nextReminder(draft: EventDraft): Reminder {
        val used = draft.reminders.map { it.daysBefore }.toSet()
        val days = REMINDER_DAYS.firstOrNull { it !in used } ?: REMINDER_DAYS.last()
        return Reminder(days, draft.reminders.lastOrNull()?.time ?: TimeOfDay.MORNING)
    }
}
