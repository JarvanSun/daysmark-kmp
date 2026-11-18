package com.example.daysmark.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.datetime.LocalDate

class CalendarDateTest {
    @Test
    fun aDayThatDoesNotExistCannotBeCreated() {
        assertFailsWith<IllegalArgumentException> { CalendarDate(2026, 2, 30) }
        assertFailsWith<IllegalArgumentException> { CalendarDate(2026, 2, 29) }
        assertFailsWith<IllegalArgumentException> { CalendarDate(2026, 13, 1) }
    }

    @Test
    fun aLeapDayExistsInLeapYears() {
        assertEquals(LocalDate(2028, 2, 29), CalendarDate(2028, 2, 29).toLocalDate())
    }

    @Test
    fun convertsFromALocalDate() {
        assertEquals(CalendarDate(2027, 1, 1), CalendarDate.of(LocalDate(2027, 1, 1)))
    }
}

class EventModelTest {
    @Test
    fun aRangeCannotEndBeforeItStarts() {
        assertFailsWith<IllegalArgumentException> {
            Event(EventId("e"), "e", CalendarDate(2026, 12, 5), endDate = CalendarDate(2026, 12, 1))
        }
    }

    @Test
    fun timesAndRemindersRejectImpossibleValues() {
        assertFailsWith<IllegalArgumentException> { TimeOfDay(24, 0) }
        assertFailsWith<IllegalArgumentException> {
            Reminder(daysBefore = -1, time = TimeOfDay.MORNING)
        }
    }
}
