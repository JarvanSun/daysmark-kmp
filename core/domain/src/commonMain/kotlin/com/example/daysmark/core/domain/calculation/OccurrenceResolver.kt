package com.example.daysmark.core.domain.calculation

import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.RepeatRule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/**
 * Which day an event counts towards on [today]. Strict: a monthly event on the 31st skips shorter
 * months, and a yearly event on 29 February happens only in leap years; a day is never moved.
 */
public object OccurrenceResolver {
    private const val MONTHS_PER_YEAR = 12

    /** The occurrence to count towards: the date itself, or a repeating event's next one. */
    public fun occurrence(date: CalendarDate, repeat: RepeatRule, today: LocalDate): LocalDate {
        val start = date.toLocalDate()
        return if (repeat == RepeatRule.NEVER || start >= today) start
        else nextOnOrAfter(start, repeat, today)
    }

    /**
     * The first month (or year) at or after [today] that has the start's day. Ends: every month has
     * days 1–28, every year has every month, and 29 February comes at least every eight years.
     */
    private fun nextOnOrAfter(start: LocalDate, repeat: RepeatRule, today: LocalDate): LocalDate {
        val step = if (repeat == RepeatRule.YEARLY) MONTHS_PER_YEAR else 1
        var monthIndex = today.year * MONTHS_PER_YEAR + today.month.number - 1
        // Align a yearly event to its own month, at or before today's month.
        if (repeat == RepeatRule.YEARLY) {
            monthIndex -= (today.month.number - start.month.number).mod(MONTHS_PER_YEAR)
        }
        while (true) {
            val year = monthIndex / MONTHS_PER_YEAR
            val month = monthIndex % MONTHS_PER_YEAR + 1
            val candidate = runCatching { LocalDate(year, month, start.day) }.getOrNull()
            if (candidate != null && candidate >= today) return candidate
            monthIndex += step
        }
    }
}
