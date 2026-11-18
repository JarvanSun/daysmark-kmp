package com.example.daysmark.core.domain.model

/**
 * A notification [daysBefore] days before an occurrence (0 means on the day), at [time]. For a
 * range, the occurrence is its start.
 */
public data class Reminder(val daysBefore: Int, val time: TimeOfDay) {
    init {
        require(daysBefore in 0..MAX_DAYS_BEFORE) { "daysBefore $daysBefore out of range" }
    }

    public companion object {
        public const val MAX_DAYS_BEFORE: Int = 365
    }
}
