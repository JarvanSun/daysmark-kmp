package com.example.daysmark.core.domain.reminders

import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.Reminder
import com.example.daysmark.core.domain.model.RepeatRule
import com.example.daysmark.core.domain.model.TimeOfDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class ReminderPlannerTest {
    private val zone = TimeZone.of("Asia/Shanghai")
    private val now = LocalDateTime(2026, 11, 18, 10, 0).toInstant(zone)
    private val nine = TimeOfDay(9, 0)

    private fun event(
        id: String,
        date: CalendarDate,
        vararg reminders: Reminder,
        repeat: RepeatRule = RepeatRule.NEVER,
    ) = Event(EventId(id), id, date, repeat = repeat, reminders = reminders.toList())

    @Test
    fun aReminderFiresTheChosenDaysBeforeAtItsTime() {
        val planned =
            ReminderPlanner.plan(
                listOf(event("trip", CalendarDate(2026, 11, 25), Reminder(1, nine))),
                now,
                zone,
            )
        val reminder = planned.single()
        assertEquals(CalendarDate(2026, 11, 24), reminder.date)
        assertEquals(nine, reminder.time)
        assertEquals(DayCount.Future(1), reminder.dayCount)
    }

    @Test
    fun aPassedReminderOfAOneTimeEventIsDropped() {
        val today = event("today", CalendarDate(2026, 11, 18), Reminder(0, nine))
        assertEquals(emptyList(), ReminderPlanner.plan(listOf(today), now, zone))
    }

    @Test
    fun aPassedReminderOfARepeatingEventMovesToTheNextOccurrence() {
        val birthday =
            event("mia", CalendarDate(1998, 11, 18), Reminder(0, nine), repeat = RepeatRule.YEARLY)
        val reminder = ReminderPlanner.plan(listOf(birthday), now, zone).first()
        assertEquals(CalendarDate(2027, 11, 18), reminder.date)
        assertEquals(DayCount.Today, reminder.dayCount)
    }

    @Test
    fun aRepeatingReminderIsPlannedSeveralOccurrencesAheadSoItKeepsFiringWithoutTheApp() {
        val rent =
            event("rent", CalendarDate(2026, 1, 1), Reminder(0, nine), repeat = RepeatRule.MONTHLY)
        val dates = ReminderPlanner.plan(listOf(rent), now, zone).map { it.date }
        assertEquals(ReminderPlanner.OCCURRENCES_AHEAD, dates.size)
        assertEquals(CalendarDate(2026, 12, 1), dates.first())
        assertEquals(CalendarDate(2027, 1, 1), dates[1])
        assertEquals(dates.size, dates.toSet().size)
    }

    @Test
    fun aOneTimeEventIsPlannedOnce() {
        val trip = event("trip", CalendarDate(2026, 12, 1), Reminder(0, nine))
        assertEquals(1, ReminderPlanner.plan(listOf(trip), now, zone).size)
    }

    @Test
    fun laterTodayStillFires() {
        val evening = event("e", CalendarDate(2026, 11, 18), Reminder(0, TimeOfDay(20, 0)))
        assertEquals(
            CalendarDate(2026, 11, 18),
            ReminderPlanner.plan(listOf(evening), now, zone).single().date,
        )
    }

    @Test
    fun theSoonestRemindersAreKeptUpToTheLimit() {
        val events =
            (1..5)
                .map { day -> event("e$day", CalendarDate(2026, 12, day), Reminder(0, nine)) }
                .reversed()
        val planned = ReminderPlanner.plan(events, now, zone, limit = 3)
        assertEquals(listOf("e1", "e2", "e3"), planned.map { it.eventId.value })
    }

    @Test
    fun idsAreStableAcrossPlans() {
        val events =
            listOf(event("trip", CalendarDate(2026, 11, 25), Reminder(1, nine), Reminder(0, nine)))
        assertEquals(
            ReminderPlanner.plan(events, now, zone).map { it.id },
            ReminderPlanner.plan(events, now, zone).map { it.id },
        )
        assertEquals(2, ReminderPlanner.plan(events, now, zone).map { it.id }.toSet().size)
    }

    @Test
    fun aReminderInTheSpringForwardGapStillFiresThatDay() {
        // 02:30 doesn't exist in New York on 14 March 2027; the reminder fires that morning anyway.
        val newYork = TimeZone.of("America/New_York")
        val now = LocalDateTime(2027, 3, 13, 12, 0).toInstant(newYork)
        val trip = event("trip", CalendarDate(2027, 3, 14), Reminder(0, TimeOfDay(2, 30)))

        val reminder = ReminderPlanner.plan(listOf(trip), now, newYork).single()

        assertEquals(CalendarDate(2027, 3, 14), reminder.date)
    }
}
