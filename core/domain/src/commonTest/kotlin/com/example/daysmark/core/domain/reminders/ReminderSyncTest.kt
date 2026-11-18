package com.example.daysmark.core.domain.reminders

import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.Reminder
import com.example.daysmark.core.domain.model.TimeOfDay
import com.example.daysmark.core.testing.FakeEventRepository
import com.example.daysmark.core.testing.RecordingErrorReporter
import com.example.daysmark.core.testing.VirtualClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderSyncTest {
    private val reporter = RecordingErrorReporter()
    private val zone = TimeZone.of("Asia/Shanghai")
    private val repository = FakeEventRepository()

    /** Records every plan handed to the system. */
    private class RecordingScheduler : ReminderScheduler {
        val plans = mutableListOf<List<PlannedReminder>>()
        var failNext = false

        override suspend fun replaceAll(reminders: List<PlannedReminder>) {
            if (failNext) {
                failNext = false
                error("The system refused")
            }
            plans += reminders
        }
    }

    private fun trip(day: Int) =
        Event(
            EventId("trip"),
            "Kyoto",
            CalendarDate(2026, 11, day),
            reminders = listOf(Reminder(0, TimeOfDay(9, 0))),
        )

    @Test
    fun replansWhenEventsChangeButNotWhenNothingDid() = runTest {
        val clock =
            VirtualClock(LocalDateTime(2026, 11, 18, 10, 0).toInstant(zone), zone) { currentTime }
        val scheduler = RecordingScheduler()
        val job = ReminderSync(repository, clock, scheduler, reporter).start(backgroundScope)
        runCurrent()
        assertEquals(listOf(emptyList()), scheduler.plans)

        repository.events.value = listOf(trip(25))
        runCurrent()
        repository.events.value = listOf(trip(25))
        runCurrent()

        assertEquals(2, scheduler.plans.size)
        assertEquals(CalendarDate(2026, 11, 25), scheduler.plans.last().single().date)
        job.cancel()
    }

    @Test
    fun aClockChangeReplansFromTheNewZonesToday() = runTest {
        // 10:00 on the 19th in Shanghai: the trip's 9:00 reminder has passed. Flying to Paris it is
        // 03:00 on the 19th there, so the reminder is ahead again and must be scheduled.
        val clock =
            VirtualClock(LocalDateTime(2026, 11, 19, 10, 0).toInstant(zone), zone) { currentTime }
        val scheduler = RecordingScheduler()
        val changes = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
        repository.events.value = listOf(trip(19))
        val job =
            ReminderSync(repository, clock, scheduler, reporter).start(backgroundScope, changes)
        runCurrent()
        assertEquals(emptyList(), scheduler.plans.last())

        clock.zone = TimeZone.of("Europe/Paris")
        changes.emit(Unit)
        runCurrent()

        assertEquals(CalendarDate(2026, 11, 19), scheduler.plans.last().single().date)
        job.cancel()
    }

    @Test
    fun aFailedSchedulingIsTriedAgainWithTheNextChangeInsteadOfStoppingTheSync() = runTest {
        val clock =
            VirtualClock(LocalDateTime(2026, 11, 18, 10, 0).toInstant(zone), zone) { currentTime }
        val scheduler = RecordingScheduler().apply { failNext = true }
        val job = ReminderSync(repository, clock, scheduler, reporter).start(backgroundScope)
        runCurrent()
        assertEquals(emptyList(), scheduler.plans)

        repository.events.value = listOf(trip(25))
        runCurrent()

        assertEquals(CalendarDate(2026, 11, 25), scheduler.plans.single().single().date)
        assertTrue(job.isActive)
        job.cancel()
    }

    @Test
    fun anUnchangedPlanThatFailedIsScheduledOnTheNextLook() = runTest {
        val clock =
            VirtualClock(LocalDateTime(2026, 11, 18, 10, 0).toInstant(zone), zone) { currentTime }
        val scheduler = RecordingScheduler().apply { failNext = true }
        val changes = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
        repository.events.value = listOf(trip(25))
        val job =
            ReminderSync(repository, clock, scheduler, reporter).start(backgroundScope, changes)
        runCurrent()
        assertEquals(emptyList(), scheduler.plans)

        // Nothing changed in the data; the app just came back to the foreground.
        changes.emit(Unit)
        runCurrent()

        assertEquals(CalendarDate(2026, 11, 25), scheduler.plans.single().single().date)
        job.cancel()
    }

    @Test
    fun aZoneChangeReschedulesAnIdenticalWallClockPlan() = runTest {
        // 10:00 in Shanghai and 03:00 in Paris: the plan (25 Nov, 09:00) reads the same in both,
        // but on Android its alarms are absolute instants and must move.
        val clock =
            VirtualClock(LocalDateTime(2026, 11, 18, 10, 0).toInstant(zone), zone) { currentTime }
        val scheduler = RecordingScheduler()
        val changes = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
        repository.events.value = listOf(trip(25))
        val job =
            ReminderSync(repository, clock, scheduler, reporter).start(backgroundScope, changes)
        runCurrent()

        clock.zone = TimeZone.of("Europe/Paris")
        changes.emit(Unit)
        runCurrent()

        assertEquals(2, scheduler.plans.size)
        assertEquals(scheduler.plans[0], scheduler.plans[1])
        job.cancel()
    }
}
