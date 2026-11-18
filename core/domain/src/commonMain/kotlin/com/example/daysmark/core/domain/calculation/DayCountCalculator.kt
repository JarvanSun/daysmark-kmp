package com.example.daysmark.core.domain.calculation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** Whole calendar days from [today] to an occurrence. Never subtracts instants. */
public object DayCountCalculator {
    public fun count(occurrence: LocalDate, today: LocalDate): DayCount {
        val days = today.daysUntil(occurrence)
        return when {
            days == 0 -> DayCount.Today
            days > 0 -> DayCount.Future(days)
            else -> DayCount.Past(-days)
        }
    }
}
