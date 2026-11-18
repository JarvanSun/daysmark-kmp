package com.example.daysmark.core.domain.calculation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class DayCountCalculatorTest {
    private val today = LocalDate(2026, 11, 18)

    @Test
    fun countsWholeDaysInBothDirections() {
        assertEquals(DayCount.Today, DayCountCalculator.count(today, today))
        assertEquals(DayCount.Future(44), DayCountCalculator.count(LocalDate(2027, 1, 1), today))
        assertEquals(DayCount.Past(1), DayCountCalculator.count(LocalDate(2026, 11, 17), today))
    }

    @Test
    fun countsAcrossLeapYears() {
        assertEquals(DayCount.Future(468), DayCountCalculator.count(LocalDate(2028, 2, 29), today))
    }
}
