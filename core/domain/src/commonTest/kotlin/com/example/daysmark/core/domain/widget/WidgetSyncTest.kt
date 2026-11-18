package com.example.daysmark.core.domain.widget

import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.testing.FakeEventRepository
import com.example.daysmark.core.testing.RecordingErrorReporter
import com.example.daysmark.core.testing.VirtualClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetSyncTest {
    private val reporter = RecordingErrorReporter()
    private val zone = TimeZone.of("Asia/Shanghai")
    private val repository = FakeEventRepository()
    private val published = mutableListOf<WidgetTimeline>()
    private val publisher = WidgetPublisher { published += it }

    private val trip = Event(EventId("trip"), "Kyoto", CalendarDate(2026, 11, 20))
    private val newYear = Event(EventId("ny"), "New Year", CalendarDate(2027, 1, 1))

    @Test
    fun publishesWhenEventsOrThePinChange() = runTest {
        val clock =
            VirtualClock(LocalDateTime(2026, 11, 18, 10, 0).toInstant(zone), zone) { currentTime }
        val job = WidgetSync(repository, clock, publisher, reporter).start(backgroundScope)
        repository.events.value = listOf(trip, newYear)
        runCurrent()
        assertEquals("trip", published.last().entries.first().focus?.id?.value)

        repository.pinned.value = newYear.id
        runCurrent()
        assertEquals("ny", published.last().entries.first().focus?.id?.value)
        job.cancel()
    }

    @Test
    fun publishesAgainAfterMidnight() = runTest {
        val clock =
            VirtualClock(LocalDateTime(2026, 11, 18, 23, 0).toInstant(zone), zone) { currentTime }
        repository.events.value = listOf(trip)
        val job = WidgetSync(repository, clock, publisher, reporter).start(backgroundScope)
        runCurrent()
        val before = published.size

        advanceTimeBy(2.hours)
        runCurrent()

        assertEquals(before + 1, published.size)
        assertEquals(DayCount.Future(1), published.last().entries.first().focus?.dayCount)
        job.cancel()
    }
}
