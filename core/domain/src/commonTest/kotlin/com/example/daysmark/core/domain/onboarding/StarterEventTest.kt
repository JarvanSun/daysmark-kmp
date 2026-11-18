package com.example.daysmark.core.domain.onboarding

import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.RepeatRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class StarterEventTest {
    private val today = LocalDate(2026, 12, 18)
    private val born = CalendarDate(1996, 3, 3)

    @Test
    fun newYearIsTheNextFirstOfJanuaryEveryYear() {
        val event = StarterEvent.NEXT_NEW_YEAR.event("New Year", today, null)!!
        assertEquals(CalendarDate(2027, 1, 1), event.date)
        assertEquals(RepeatRule.YEARLY, event.repeat)
    }

    @Test
    fun nextMonthCrossesTheYearAndRepeatsMonthly() {
        val event = StarterEvent.NEXT_MONTH.event("Next Month", today, null)!!
        assertEquals(CalendarDate(2027, 1, 1), event.date)
        assertEquals(RepeatRule.MONTHLY, event.repeat)
    }

    @Test
    fun theBirthdayRepeatsAndRemindsAWeekADayAndOnTheDay() {
        val event = StarterEvent.BIRTHDAY.event("My Birthday", today, born)!!
        assertEquals(born, event.date)
        assertEquals(RepeatRule.YEARLY, event.repeat)
        assertEquals(EventCategory.BIRTHDAY, event.category)
        assertEquals(listOf(7, 1, 0), event.reminders.map { it.daysBefore })
    }

    @Test
    fun theBirthDateCountsUpWithoutRepeating() {
        assertEquals(RepeatRule.NEVER, StarterEvent.BIRTH_DATE.event("Born", today, born)!!.repeat)
    }

    @Test
    fun birthdayStartersNeedABirthday() {
        assertNull(StarterEvent.BIRTHDAY.event("My Birthday", today, null))
        assertNull(StarterEvent.BIRTH_DATE.event("Born", today, null))
    }

    @Test
    fun idsAreFixedSoFinishingTwiceReplacesInsteadOfDuplicating() {
        assertEquals(
            StarterEvent.NEXT_NEW_YEAR.event("a", today, null)!!.id,
            StarterEvent.NEXT_NEW_YEAR.event("b", LocalDate(2030, 5, 5), null)!!.id,
        )
    }
}
