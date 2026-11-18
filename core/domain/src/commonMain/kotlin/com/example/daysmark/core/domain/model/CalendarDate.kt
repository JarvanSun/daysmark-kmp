package com.example.daysmark.core.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/**
 * A Gregorian day as the user saw it: year, month, day, never an instant or a time zone, so an
 * event stays on the same calendar day everywhere. Always a day that exists: 30 February can't be
 * constructed, so no calculation ever has to move a date that doesn't exist.
 */
public data class CalendarDate(val year: Int, val month: Int, val day: Int) {
    init {
        require(runCatching { LocalDate(year, month, day) }.isSuccess) {
            "$year-$month-$day is not a calendar date"
        }
    }

    public fun toLocalDate(): LocalDate = LocalDate(year, month, day)

    public companion object {
        public fun of(date: LocalDate): CalendarDate =
            CalendarDate(date.year, date.month.number, date.day)
    }
}
