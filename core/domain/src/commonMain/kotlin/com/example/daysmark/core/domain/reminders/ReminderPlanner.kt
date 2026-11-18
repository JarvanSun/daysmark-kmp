package com.example.daysmark.core.domain.reminders

import com.example.daysmark.core.domain.calculation.CountedEvent
import com.example.daysmark.core.domain.calculation.OccurrenceResolver
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.Reminder
import com.example.daysmark.core.domain.model.RepeatRule
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Which notifications should be pending now: every reminder's upcoming firings, soonest first, at
 * most [limit] in total (iOS keeps 64 pending notifications per app). A repeating event's reminder
 * is planned for several occurrences ahead, because iOS doesn't wake the app when one is delivered:
 * with only the next one planned, a monthly reminder would fire once and go quiet until the app is
 * opened again. Pure: the same events and moment always give the same plan, so re-planning on every
 * change is safe.
 */
public object ReminderPlanner {
    public const val DEFAULT_LIMIT: Int = 64

    /** Occurrences planned ahead per repeating reminder; the overall [limit] still applies. */
    public const val OCCURRENCES_AHEAD: Int = 12

    public fun plan(
        events: List<Event>,
        now: Instant,
        zone: TimeZone,
        limit: Int = DEFAULT_LIMIT,
    ): List<PlannedReminder> =
        events
            .flatMap { event ->
                event.reminders.withIndex().flatMap { (index, reminder) ->
                    upcoming(event, index, reminder, now, zone)
                }
            }
            .sortedWith(compareBy({ it.second }, { it.first.id }))
            .take(limit)
            .map { it.first }

    /** The reminder's firings after [now], each paired with its instant for ordering. */
    private fun upcoming(
        event: Event,
        index: Int,
        reminder: Reminder,
        now: Instant,
        zone: TimeZone,
    ): List<Pair<PlannedReminder, Instant>> {
        val today = now.toLocalDateTime(zone).date
        // The first occurrence whose reminder could still be ahead: the one on or after
        // `daysBefore` days from today, so its firing is today or later.
        val first =
            OccurrenceResolver.occurrence(
                event.date,
                event.repeat,
                today.plus(DatePeriod(days = reminder.daysBefore)),
            )
        val occurrences =
            if (event.repeat == RepeatRule.NEVER) sequenceOf(first)
            else
                generateSequence(first) {
                    OccurrenceResolver.occurrence(
                        event.date,
                        event.repeat,
                        it.plus(DatePeriod(days = 1)),
                    )
                }
        return occurrences
            .mapNotNull { occurrence -> firing(event, index, reminder, occurrence, zone) }
            .filter { (_, fireAt) -> fireAt > now }
            .take(if (event.repeat == RepeatRule.NEVER) 1 else OCCURRENCES_AHEAD)
            .toList()
    }

    private fun firing(
        event: Event,
        index: Int,
        reminder: Reminder,
        occurrence: LocalDate,
        zone: TimeZone,
    ): Pair<PlannedReminder, Instant> {
        val day = occurrence.minus(DatePeriod(days = reminder.daysBefore))
        val fireAt = day.atTime(reminder.time.toLocalTime()).toInstant(zone)
        val counted = CountedEvent.of(event, fireAt, zone)
        return PlannedReminder(
            id = "${event.id.value}#$index#$occurrence",
            eventId = event.id,
            title = event.title,
            date = CalendarDate.of(day),
            time = reminder.time,
            dayCount = counted.dayCount,
            phase = counted.phase,
        ) to fireAt
    }
}
