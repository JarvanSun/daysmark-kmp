package com.example.daysmark.core.domain.reminders

import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.calculation.RangePhase
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.TimeOfDay

/**
 * One notification to schedule. The time is wall-clock ([date] at [time] wherever the user is); the
 * text is the event's title and the phrase for that moment, which the platform localizes.
 */
public data class PlannedReminder(
    /** Stable across re-planning, so the same notification is replaced, never duplicated. */
    val id: String,
    val eventId: EventId,
    val title: String,
    val date: CalendarDate,
    val time: TimeOfDay,
    val dayCount: DayCount,
    val phase: RangePhase,
)

/** Schedules local notifications; one implementation per platform. */
public interface ReminderScheduler {
    /**
     * Replaces every pending Daysmark notification with [reminders]. Returns once the system holds
     * exactly these; throws if it refused (the implementation logs why). Called one at a time.
     */
    @Throws(Exception::class) public suspend fun replaceAll(reminders: List<PlannedReminder>)
}
