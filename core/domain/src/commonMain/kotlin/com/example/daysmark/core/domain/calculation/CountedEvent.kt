package com.example.daysmark.core.domain.calculation

import com.example.daysmark.core.domain.model.Event
import kotlin.math.abs
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * An event as it stands at one moment: the occurrence it refers to, where a range is, and the
 * distance every surface shows. All inputs are explicit (docs/spec/architecture.md).
 */
public data class CountedEvent(
    val event: Event,
    /** The occurrence's start: the date itself, or a repeating event's current one. */
    val occurrence: LocalDate,
    /** A range occurrence's last day; `null` for a single day. */
    val occurrenceEnd: LocalDate?,
    val phase: RangePhase,
    /** The day the distance is measured to: the start, or a range's end once it has begun. */
    val target: LocalDate,
    val dayCount: DayCount,
) {
    public companion object {
        private const val SECONDS_PER_MINUTE = 60
        private const val SECONDS_PER_HOUR = 3_600

        public fun of(event: Event, now: Instant, zone: TimeZone): CountedEvent {
            val today = now.toLocalDateTime(zone).date
            val length = event.endDate?.let { event.date.toLocalDate().daysUntil(it.toLocalDate()) }
            // A range stays current until its end has passed: look for the occurrence whose end is
            // on or after today, which is the start on or after `today - length`.
            val searchFrom = if (length == null) today else today.minus(DatePeriod(days = length))
            val start = OccurrenceResolver.occurrence(event.date, event.repeat, searchFrom)
            val end = length?.let { start.plus(DatePeriod(days = it)) }
            // A timed range begins at its start time, not at midnight of its first day.
            val startsAt = event.time?.let { start.atTime(it.toLocalTime()).toInstant(zone) }
            val phase = phase(start, end, startsAt, today, now)
            val target = if (end != null && phase != RangePhase.BEFORE_START) end else start
            // The time belongs to the start; a range's end counts in days.
            val time = event.time.takeIf { target == start }
            val dayCount =
                if (time != null && target == today) {
                    moment(target.atTime(time.toLocalTime()).toInstant(zone), now)
                } else {
                    DayCountCalculator.count(target, today)
                }
            return CountedEvent(event, start, end, phase, target, dayCount)
        }

        private fun phase(
            start: LocalDate,
            end: LocalDate?,
            startsAt: Instant?,
            today: LocalDate,
            now: Instant,
        ): RangePhase =
            when {
                end == null -> RangePhase.POINT
                today < start -> RangePhase.BEFORE_START
                today == start && startsAt != null && now < startsAt -> RangePhase.BEFORE_START
                today <= end -> RangePhase.ONGOING
                else -> RangePhase.ENDED
            }

        /**
         * Hours on the day, minutes in the last hour; [DayCount.Now] within the minute. Rounded so
         * the phrase never overstates: up while it is ahead ("in 1 hour" at 59 minutes), down once
         * it has passed ("1 hour ago" at 61 minutes).
         */
        private fun moment(target: Instant, now: Instant): DayCount {
            val seconds = (target - now).inWholeSeconds
            val isFuture = seconds > 0
            val magnitude = abs(seconds)
            fun inUnits(unit: Int) =
                (if (isFuture) (magnitude + unit - 1) / unit else magnitude / unit).toInt()
            return when {
                magnitude < SECONDS_PER_MINUTE -> DayCount.Now
                magnitude < SECONDS_PER_HOUR ->
                    DayCount.Minutes(inUnits(SECONDS_PER_MINUTE), isFuture)
                else -> DayCount.Hours(inUnits(SECONDS_PER_HOUR), isFuture)
            }
        }
    }
}
