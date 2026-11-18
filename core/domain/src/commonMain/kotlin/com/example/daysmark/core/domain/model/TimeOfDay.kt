package com.example.daysmark.core.domain.model

import kotlinx.datetime.LocalTime

/**
 * A wall-clock time, without a date or zone: an event at 19:30 is at 19:30 wherever the user is.
 */
public data class TimeOfDay(val hour: Int, val minute: Int) {
    init {
        require(hour in 0..MAX_HOUR && minute in 0..MAX_MINUTE) { "$hour:$minute is not a time" }
    }

    public fun toLocalTime(): LocalTime = LocalTime(hour, minute)

    public companion object {
        private const val MAX_HOUR = 23
        private const val MAX_MINUTE = 59

        /** Where a new reminder starts: nine in the morning. */
        public val MORNING: TimeOfDay = TimeOfDay(9, 0)
    }
}
