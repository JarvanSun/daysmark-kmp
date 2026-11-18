package com.example.daysmark.core.domain.time

import com.example.daysmark.core.testing.VirtualClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DayChangesTest {
    @Test
    fun emitsTodayAndThenEachNewDayAtMidnight() = runTest {
        val zone = TimeZone.of("Asia/Shanghai")
        val lateEvening = LocalDateTime(2026, 11, 18, 23, 0).toInstant(zone)
        val clock = VirtualClock(lateEvening, zone) { currentTime }

        val days = clock.days().take(3).toList()

        assertEquals(
            listOf(LocalDate(2026, 11, 18), LocalDate(2026, 11, 19), LocalDate(2026, 11, 20)),
            days,
        )
        // Two midnights crossed: about 25 hours of virtual time, never real waiting.
        assertEquals(
            true,
            currentTime in 25.hours.inWholeMilliseconds..26.hours.inWholeMilliseconds,
        )
    }

    @Test
    fun minutesTickOnEveryMinuteBoundary() = runTest {
        val zone = TimeZone.UTC
        val clock =
            VirtualClock(LocalDateTime(2026, 11, 18, 9, 0, 40).toInstant(zone), zone) {
                currentTime
            }

        val ticks = clock.minutes().take(3).toList()

        assertEquals(
            listOf(
                LocalDateTime(2026, 11, 18, 9, 0, 40),
                LocalDateTime(2026, 11, 18, 9, 1, 0),
                LocalDateTime(2026, 11, 18, 9, 2, 0),
            ),
            ticks.map { it.toLocalDateTime(zone) },
        )
    }

    @Test
    fun secondsTickEverySecond() = runTest {
        val clock = VirtualClock(Instant.fromEpochMilliseconds(1_500), TimeZone.UTC) { currentTime }
        assertEquals(
            listOf(1_500L, 2_000L, 3_000L),
            clock.seconds().take(3).toList().map { it.toEpochMilliseconds() },
        )
    }

    @Test
    fun aClockChangeMeasuresTheWaitForMidnightAgainInTheNewZone() = runTest {
        // 23:00 in Shanghai is 16:00 in Paris: after flying, the next day starts 8 hours later.
        val shanghai = TimeZone.of("Asia/Shanghai")
        val clock =
            VirtualClock(LocalDateTime(2026, 11, 18, 23, 0).toInstant(shanghai), shanghai) {
                currentTime
            }
        val changes = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
        val seen = mutableListOf<LocalDate>()
        val job = launch { clock.daysAcross(changes).collect { seen += it } }
        runCurrent()

        clock.zone = TimeZone.of("Europe/Paris")
        changes.emit(Unit)
        advanceTimeBy(2.hours)
        runCurrent()
        // Without the restart the old wait would have fired at Shanghai midnight (one hour in).
        assertEquals(listOf(LocalDate(2026, 11, 18)), seen.distinct())

        advanceTimeBy(7.hours)
        runCurrent()
        assertEquals(LocalDate(2026, 11, 19), seen.last())
        job.cancel()
    }
}
