package com.example.daysmark.core.domain.calculation

import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.RepeatRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class OccurrenceResolverTest {
    private val today = LocalDate(2026, 11, 18)

    private fun occurrence(y: Int, m: Int, d: Int, repeat: RepeatRule, on: LocalDate = today) =
        OccurrenceResolver.occurrence(CalendarDate(y, m, d), repeat, on)

    @Test
    fun oneTimeEventsCountTowardsTheirOwnDateEvenWhenPast() {
        assertEquals(LocalDate(2025, 10, 7), occurrence(2025, 10, 7, RepeatRule.NEVER))
    }

    @Test
    fun futureRepeatingEventsStartOnTheirOwnDate() {
        assertEquals(LocalDate(2027, 3, 1), occurrence(2027, 3, 1, RepeatRule.YEARLY))
    }

    @Test
    fun yearlyEventsMoveToThisYearOrNext() {
        assertEquals(LocalDate(2026, 11, 25), occurrence(1998, 11, 25, RepeatRule.YEARLY))
        assertEquals(LocalDate(2027, 10, 25), occurrence(1998, 10, 25, RepeatRule.YEARLY))
    }

    @Test
    fun theOccurrenceDayItselfIsToday() {
        assertEquals(today, occurrence(1990, 11, 18, RepeatRule.YEARLY))
        assertEquals(today, occurrence(2026, 1, 18, RepeatRule.MONTHLY))
    }

    @Test
    fun yearlyOnTheTwentyNinthOfFebruaryWaitsForALeapYear() {
        assertEquals(LocalDate(2028, 2, 29), occurrence(2024, 2, 29, RepeatRule.YEARLY))
    }

    @Test
    fun yearlyLeapDaySkipsTheNonLeapCenturyYear() {
        val march2096 = LocalDate(2096, 3, 1)
        assertEquals(
            LocalDate(2104, 2, 29),
            occurrence(2024, 2, 29, RepeatRule.YEARLY, on = march2096),
        )
    }

    @Test
    fun monthlyOnTheThirtyFirstSkipsShorterMonths() {
        // 30 November has no 31st; the next is 31 December.
        assertEquals(LocalDate(2026, 12, 31), occurrence(2026, 1, 31, RepeatRule.MONTHLY))
    }
}
