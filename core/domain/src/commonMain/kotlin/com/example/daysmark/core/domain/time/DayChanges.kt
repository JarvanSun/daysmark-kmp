package com.example.daysmark.core.domain.time

import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Today's date, then the new date at every midnight in the clock's zone. */
public fun WallClock.today(): LocalDate = now().toLocalDateTime(timeZone()).date

public fun WallClock.days(): Flow<LocalDate> = flow {
    while (true) {
        val today = today()
        emit(today)
        val midnight = today.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone())
        // A second past midnight, so the zone's new day has certainly begun.
        delay(midnight - now() + 1.seconds)
    }
}
    .distinctUntilChanged()

/**
 * [days], started again whenever [clockChanges] emits. Waiting for midnight is a single long delay;
 * after the time or zone changes, or after the device slept through midnight, it must be measured
 * again from the new clock.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public fun WallClock.daysAcross(clockChanges: Flow<Unit>): Flow<LocalDate> =
    clockChanges.flatMapLatest {
        days()
    }

/**
 * The current moment, then again at every minute boundary: Home's timed events change by minute.
 */
public fun WallClock.minutes(): Flow<Instant> = ticks(period = 60.seconds)

/** The current moment, then every second: the detail screen's ticking line. */
public fun WallClock.seconds(): Flow<Instant> = ticks(period = 1.seconds)

private fun WallClock.ticks(period: kotlin.time.Duration): Flow<Instant> = flow {
    while (true) {
        val now = now()
        emit(now)
        val elapsed = now.toEpochMilliseconds().mod(period.inWholeMilliseconds)
        delay(period.inWholeMilliseconds - elapsed)
    }
}
