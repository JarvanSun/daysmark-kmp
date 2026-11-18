package com.example.daysmark.core.domain.widget

import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.TimeOfDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant

class WidgetTimelineTest {
    private val zone = TimeZone.of("Asia/Shanghai")
    private val now = LocalDateTime(2026, 11, 18, 15, 30).toInstant(zone)
    private val trip = Event(EventId("trip"), "Kyoto", CalendarDate(2026, 11, 20))
    private val newYear = Event(EventId("ny"), "New Year", CalendarDate(2027, 1, 1))

    @Test
    fun startsNowThenOneEntryPerMidnight() {
        val timeline = WidgetTimeline.of(listOf(trip), null, now, zone)
        assertEquals(WidgetTimeline.DAYS + 1, timeline.entries.size)
        assertEquals(now, timeline.entries.first().startsAt)
        assertEquals(LocalDate(2026, 11, 19).atStartOfDayIn(zone), timeline.entries[1].startsAt)
    }

    @Test
    fun eachEntryCountsFromItsOwnDay() {
        val counts =
            WidgetTimeline.of(listOf(trip), null, now, zone).entries.take(4).map {
                it.focus?.dayCount
            }
        assertEquals(
            listOf(DayCount.Future(2), DayCount.Future(1), DayCount.Today, DayCount.Past(1)),
            counts,
        )
    }

    @Test
    fun theFocusMovesOnAsTheNearestEventChanges() {
        val entries = WidgetTimeline.of(listOf(trip, newYear), null, now, zone).entries
        assertEquals("trip", entries.first().focus?.id?.value)
        // A week on, New Year (37 days) is still farther than the trip that passed 5 days ago.
        assertEquals("trip", entries[7].focus?.id?.value)
        // A month on, New Year (14 days) is nearer than the trip (28 days ago).
        assertEquals("ny", entries.last().focus?.id?.value)
    }

    @Test
    fun thePinnedEventIsTheFocusThroughout() {
        val entries = WidgetTimeline.of(listOf(trip, newYear), EventId("ny"), now, zone).entries
        assertEquals(setOf("ny"), entries.map { it.focus?.id?.value }.toSet())
    }

    @Test
    fun withoutEventsThereIsNoFocus() {
        assertNull(WidgetTimeline.of(emptyList(), null, now, zone).entries.first().focus)
    }

    @Test
    fun aTimedEventLaterTodayReadsAsTodayBecauseTheWidgetOnlyRefreshesAtMidnight() {
        val dinner =
            Event(EventId("d"), "Dinner", CalendarDate(2026, 11, 18), time = TimeOfDay(19, 0))
        assertEquals(
            DayCount.Today,
            WidgetTimeline.of(listOf(dinner), null, now, zone).entries.first().focus?.dayCount,
        )
    }
}
