package com.example.daysmark.core.testing

import com.example.daysmark.core.domain.time.WallClock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

/** A clock stopped at the start of [today] in [zone]. */
public class FixedWallClock(today: LocalDate, private val zone: TimeZone = TimeZone.UTC) :
    WallClock {
    private val instant: Instant = today.atStartOfDayIn(zone)

    override fun now(): Instant = instant

    override fun timeZone(): TimeZone = zone
}
