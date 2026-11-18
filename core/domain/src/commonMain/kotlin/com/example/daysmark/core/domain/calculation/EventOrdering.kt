package com.example.daysmark.core.domain.calculation

import com.example.daysmark.core.domain.model.EventId

/** The one order every surface (Home, widget) uses, and how the focused event is chosen. */
public object EventOrdering {
    /**
     * Today (all-day first, then by time of day, like an agenda), then the future nearest first,
     * then the past most recent first; ties by title, ID.
     */
    public val listOrder: Comparator<CountedEvent> =
        compareBy<CountedEvent>(
                { it.dayCount.rank() },
                { it.dayCount.distance() },
                { it.minuteOfToday() },
            )
            .thenBy { it.event.title.lowercase() }
            .thenBy { it.event.id.value }

    /**
     * The pinned event if it still exists, otherwise the event nearest to now either way: whole
     * days first, then, among today's, the one closest in minutes.
     */
    public fun focus(events: List<CountedEvent>, pinned: EventId?): CountedEvent? =
        events.firstOrNull { it.event.id == pinned }
            ?: events.minWithOrNull(
                compareBy<CountedEvent>(
                        { it.dayCount.distance() },
                        { it.dayCount.minutesAway() },
                        { it.dayCount.rank() },
                    )
                    .then(listOrder)
            )

    /** Which group an event is listed in, in list order. */
    private enum class Group {
        Today,
        Future,
        Past,
    }

    private fun DayCount.rank(): Int =
        when (this) {
            is DayCount.Future -> Group.Future
            is DayCount.Past -> Group.Past
            else -> Group.Today
        }.ordinal

    /** Minutes from midnight of a timed event on today's date; -1 (first) for an all-day one. */
    private fun CountedEvent.minuteOfToday(): Int {
        val time = event.time?.takeIf { dayCount.isToday && target == occurrence } ?: return -1
        return time.hour * MINUTES_PER_HOUR + time.minute
    }

    /** How close a same-day count is, in minutes; 0 for all-day today and for other days. */
    private fun DayCount.minutesAway(): Int =
        when (this) {
            is DayCount.Hours -> hours * MINUTES_PER_HOUR
            is DayCount.Minutes -> minutes
            else -> 0
        }

    private const val MINUTES_PER_HOUR = 60

    /** Whole days away; everything on today's date is 0. */
    private fun DayCount.distance(): Int =
        when (this) {
            is DayCount.Future -> days
            is DayCount.Past -> days
            else -> 0
        }
}
