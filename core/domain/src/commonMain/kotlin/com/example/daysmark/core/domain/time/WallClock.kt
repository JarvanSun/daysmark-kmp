package com.example.daysmark.core.domain.time

import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/** The current moment and zone, passed in so no calculation reads the system clock itself. */
public interface WallClock {
    public fun now(): Instant

    public fun timeZone(): TimeZone
}
