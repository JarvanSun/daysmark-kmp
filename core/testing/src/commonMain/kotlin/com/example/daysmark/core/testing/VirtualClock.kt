package com.example.daysmark.core.testing

import com.example.daysmark.core.domain.time.WallClock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/**
 * A clock that moves with a coroutine test's virtual time: [elapsedMillis] is the test scheduler's
 * `currentTime`. [zone] can be changed, as when the user travels.
 */
public class VirtualClock(
    private val start: Instant,
    public var zone: TimeZone,
    private val elapsedMillis: () -> Long,
) : WallClock {
    override fun now(): Instant = start + elapsedMillis().milliseconds

    override fun timeZone(): TimeZone = zone
}
