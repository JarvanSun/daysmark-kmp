package com.example.daysmark.core.domain.calculation

import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.TimeOfDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant

class EventOrderingTest {
    private val today = LocalDate(2026, 11, 18)

    private fun counted(title: String, month: Int, day: Int, year: Int = 2026) =
        CountedEvent.of(
            Event(EventId(title), title, CalendarDate(year, month, day)),
            today.atStartOfDayIn(TimeZone.UTC),
            TimeZone.UTC,
        )

    private val past = counted("past", 11, 10)
    private val recent = counted("recent", 11, 17)
    private val today0 = counted("today", 11, 18)
    private val near = counted("near", 11, 20)
    private val far = counted("far", 12, 25)

    @Test
    fun listsTodayThenFutureNearestFirstThenPastMostRecentFirst() {
        val ordered = listOf(past, far, recent, near, today0).sortedWith(EventOrdering.listOrder)
        assertEquals(
            listOf("today", "near", "far", "recent", "past"),
            ordered.map { it.event.title },
        )
    }

    @Test
    fun equalDistancesAreOrderedByTitle() {
        val b = counted("Banana", 12, 1)
        val a = counted("apple", 12, 1)
        assertEquals(listOf(a, b), listOf(b, a).sortedWith(EventOrdering.listOrder))
    }

    @Test
    fun focusIsTheNearestEventInEitherDirection() {
        assertEquals(recent, EventOrdering.focus(listOf(far, recent, near), pinned = null))
    }

    @Test
    fun anUpcomingEventWinsATieWithAPastOne() {
        val tomorrow = counted("tomorrow", 11, 19)
        assertEquals(tomorrow, EventOrdering.focus(listOf(recent, tomorrow), pinned = null))
    }

    @Test
    fun aPinnedEventIsTheFocusWhateverItsDistance() {
        assertEquals(far, EventOrdering.focus(listOf(near, far), pinned = EventId("far")))
    }

    @Test
    fun aPinnedEventThatNoLongerExistsFallsBackToTheNearest() {
        assertEquals(near, EventOrdering.focus(listOf(near, far), pinned = EventId("deleted")))
    }

    @Test
    fun thereIsNoFocusWithoutEvents() {
        assertEquals(null, EventOrdering.focus(emptyList(), pinned = null))
    }

    private fun timed(title: String, hour: Int, now: Int = 12) =
        CountedEvent.of(
            Event(EventId(title), title, CalendarDate(2026, 11, 18), time = TimeOfDay(hour, 0)),
            LocalDateTime(2026, 11, 18, now, 0).toInstant(TimeZone.UTC),
            TimeZone.UTC,
        )

    @Test
    fun todayReadsLikeAnAgendaAllDayFirstThenByTime() {
        val allDay =
            CountedEvent.of(
                Event(EventId("z"), "z all day", CalendarDate(2026, 11, 18)),
                LocalDateTime(2026, 11, 18, 12, 0).toInstant(TimeZone.UTC),
                TimeZone.UTC,
            )
        val evening = timed("a evening", 20)
        val morning = timed("b morning", 9)
        assertEquals(
            listOf("z all day", "b morning", "a evening"),
            listOf(evening, morning, allDay).sortedWith(EventOrdering.listOrder).map {
                it.event.title
            },
        )
    }

    @Test
    fun amongTodaysEventsTheFocusIsTheClosestInTime() {
        // At noon: 13:00 is an hour away, 9:00 three hours ago, 20:00 eight hours ahead.
        val events = listOf(timed("a", 20), timed("b", 9), timed("c", 13))
        assertEquals("c", EventOrdering.focus(events, null)?.event?.title)
    }
}
