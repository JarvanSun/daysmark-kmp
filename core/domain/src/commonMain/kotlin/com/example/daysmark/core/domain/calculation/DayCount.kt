package com.example.daysmark.core.domain.calculation

/**
 * How far an event is from now. Platforms turn it into a localized phrase ("in 12 days", "in 3
 * hours", "Today"); the domain never produces user-facing text.
 *
 * All-day events count whole days. A timed event counts days until its day, hours on that day and
 * minutes in its last hour, and is [Now] at its moment.
 */
public sealed interface DayCount {
    public data object Today : DayCount

    public data class Future(val days: Int) : DayCount

    public data class Past(val days: Int) : DayCount

    /** On a timed event's day: whole hours to go or gone, rounded up so "in 1 hour" never lies. */
    public data class Hours(val hours: Int, val isFuture: Boolean) : DayCount

    /** In a timed event's last hour, either side. */
    public data class Minutes(val minutes: Int, val isFuture: Boolean) : DayCount

    public data object Now : DayCount

    /** Whether this is on today's date (any of the same-day cases). */
    public val isToday: Boolean
        get() = this is Today || this is Hours || this is Minutes || this is Now
}

/** Where a range stands; the phrase is placed in it ("Starts in 3 days", "Ends tomorrow"). */
public enum class RangePhase {
    /** Not a range. */
    POINT,
    BEFORE_START,
    ONGOING,
    ENDED,
}
