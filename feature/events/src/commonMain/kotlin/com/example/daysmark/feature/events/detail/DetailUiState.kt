package com.example.daysmark.feature.events.detail

import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.calculation.RangePhase
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.EventIcon
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.TimeOfDay

/** Everything an event's detail screen shows; recomputed every second for the ticking line. */
public sealed interface DetailUiState {
    public data object Loading : DetailUiState

    /** The event was deleted (here or elsewhere); the platform leaves the screen. */
    public data object Missing : DetailUiState

    /** The event couldn't be read; the platform offers [DetailAction.Retry]. */
    public data object Failed : DetailUiState

    public data class Content(
        val id: EventId,
        val title: String,
        val icon: EventIcon,
        /** The day the count refers to, with its time when it counts to the start. */
        val date: CalendarDate,
        val time: TimeOfDay?,
        val dayCount: DayCount,
        val phase: RangePhase,
        val isPinned: Boolean,
        /** The large number, in the unit the user cycled to. */
        val count: DetailCount,
        /**
         * Days, hours, minutes and seconds to or since the moment; `null` on an all-day event's
         * day.
         */
        val ticker: Elapsed?,
        /** Whether tapping the number changes its unit (not on the day itself). */
        val canSwitchUnit: Boolean,
        /** It's the day: confetti falls once. */
        val isTheDay: Boolean,
        /** Pinning couldn't be stored; cleared by [DetailAction.DismissFailure]. */
        val pinFailed: Boolean = false,
    ) : DetailUiState
}

public enum class DetailUnit {
    DAYS,
    WEEKS,
    HOURS,
    MINUTES,
}

public enum class CountDirection {
    UPCOMING,
    TODAY,
    PAST,
}

/** "547 days since", "78 weeks since, and 1 day". */
public data class DetailCount(
    val value: Long,
    val unit: DetailUnit,
    val direction: CountDirection,
    /** Days left over when counting in weeks; 0 otherwise. */
    val remainderDays: Int,
)

public data class Elapsed(val days: Long, val hours: Int, val minutes: Int, val seconds: Int)
