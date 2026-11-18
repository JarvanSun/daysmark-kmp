package com.example.daysmark.feature.events.detail

import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.TimeOfDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class DetailContentTest {
    private val zone = TimeZone.of("Asia/Shanghai")
    private val firstDate = Event(EventId("d"), "First date", CalendarDate(2025, 4, 8))
    private val now = LocalDateTime(2026, 10, 7, 9, 41, 0).toInstant(zone)

    /** The content after cycling to [unit] (days → weeks → hours → minutes on an ordinary day). */
    private fun at(unit: DetailUnit, event: Event = firstDate, moment: kotlin.time.Instant = now) =
        content(event, isPinned = false, moment, zone, cycles = DetailUnit.entries.indexOf(unit))

    @Test
    fun countsDaysSinceAndTicksFromTheStartOfThatDay() {
        val days = at(DetailUnit.DAYS)
        assertEquals(DetailCount(547, DetailUnit.DAYS, CountDirection.PAST, 0), days.count)
        assertEquals(Elapsed(days = 547, hours = 9, minutes = 41, seconds = 0), days.ticker)
    }

    @Test
    fun weeksKeepTheRemainingDays() {
        assertEquals(
            DetailCount(78, DetailUnit.WEEKS, CountDirection.PAST, 1),
            at(DetailUnit.WEEKS).count,
        )
    }

    @Test
    fun hoursAndMinutesCountTheWholeDuration() {
        assertEquals(13_137, at(DetailUnit.HOURS).count.value)
        assertEquals(788_261, at(DetailUnit.MINUTES).count.value)
    }

    @Test
    fun onAnAllDayEventsDayItIsTheDayWithoutATicker() {
        val today = at(DetailUnit.HOURS, Event(EventId("t"), "Today", CalendarDate(2026, 10, 7)))
        assertEquals(CountDirection.TODAY, today.count.direction)
        assertEquals(DetailUnit.DAYS, today.count.unit)
        assertNull(today.ticker)
        assertFalse(today.canSwitchUnit)
        assertTrue(today.isTheDay)
    }

    @Test
    fun aTimedEventTodayTicksDownToItsTime() {
        val dinner =
            Event(EventId("x"), "Dinner", CalendarDate(2026, 10, 7), time = TimeOfDay(19, 30))
        val content = at(DetailUnit.DAYS, dinner)
        assertEquals(Elapsed(0, 9, 49, 0), content.ticker)
        assertEquals(CountDirection.UPCOMING, content.count.direction)
        assertTrue(content.isTheDay)
        // Never "0 days": on its day a timed event counts in hours (or minutes) only.
        // 9 h 49 m ahead reads "10 hours", as Home's "in 10 hours" does.
        assertEquals(DetailCount(10, DetailUnit.HOURS, CountDirection.UPCOMING, 0), content.count)
        assertEquals(DetailUnit.MINUTES, at(DetailUnit.MINUTES, dinner).count.unit)
        assertEquals(
            listOf(DetailUnit.HOURS, DetailUnit.MINUTES),
            unitsFor(content.count.direction, content.dayCount),
        )
    }

    @Test
    fun atTheMomentItselfItIsToday() {
        val dinner =
            Event(EventId("x"), "Dinner", CalendarDate(2026, 10, 7), time = TimeOfDay(9, 41))
        val content = at(DetailUnit.HOURS, dinner)
        assertEquals(CountDirection.TODAY, content.count.direction)
        assertFalse(content.canSwitchUnit)
    }
}
