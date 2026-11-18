package com.example.daysmark.shared

import com.example.daysmark.core.domain.time.WallClock
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/** The device's clock and zone, read on every call so a zone change is picked up. */
internal object SystemWallClock : WallClock {
    override fun now(): Instant = Clock.System.now()

    override fun timeZone(): TimeZone = TimeZone.currentSystemDefault()
}
