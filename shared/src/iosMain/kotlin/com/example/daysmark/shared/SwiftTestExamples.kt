package com.example.daysmark.shared

import com.example.daysmark.core.domain.calculation.DayCount

/**
 * One example of every case of the shared sealed types, for the Swift mapping tests
 * (`iosApp/DaysmarkTests`). Swift can't enumerate a Kotlin sealed type, so the list lives here,
 * next to an exhaustive `when`: a new `DayCount` subtype doesn't compile until it has an example,
 * and then the Swift tests check its mapping. Enums need nothing: Swift reads their `entries`.
 */
public object SwiftTestExamples {
    public val dayCounts: List<DayCount> =
        listOf(
                DayCount.Today,
                DayCount.Now,
                DayCount.Future(3),
                DayCount.Past(4),
                DayCount.Hours(2, isFuture = true),
                DayCount.Minutes(5, isFuture = false),
            )
            .onEach(::covered)

    /** Doesn't compile when a case is missing; [dayCounts] must have an example of each. */
    private fun covered(count: DayCount): Unit =
        when (count) {
            DayCount.Today,
            DayCount.Now,
            is DayCount.Future,
            is DayCount.Past,
            is DayCount.Hours,
            is DayCount.Minutes -> Unit
        }
}
