package com.example.daysmark.feature.events.home

import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.calculation.RangePhase
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventIcon
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.TimeOfDay

/** Everything Home shows, as one immutable value. Platforms render it; they never compute it. */
public sealed interface HomeUiState {
    public data object Loading : HomeUiState

    /** The events couldn't be read; the platform offers [HomeAction.Retry]. */
    public data object Failed : HomeUiState

    public data class Content(
        /** The pinned or nearest event; `null` only when there are no events. */
        val focus: EventRow?,
        val landscape: LandscapeCounts,
        /** "All" (category `null`) first, then the built-in categories. */
        val filters: List<CategoryFilter>,
        /** The events matching the selected filter, in list order. */
        val rows: List<EventRow>,
        val placeholder: HomePlaceholder?,
        /**
         * A change the user asked for couldn't be stored; cleared by [HomeAction.DismissFailure].
         */
        val failure: HomeFailure? = null,
    ) : HomeUiState {
        /** The focus card, landscape and filters show only once there is an event. */
        val showsOverview: Boolean
            get() = focus != null
    }
}

/** One event as Home shows it. */
public data class EventRow(
    val id: EventId,
    val title: String,
    val icon: EventIcon,
    val category: EventCategory,
    val dayCount: DayCount,
    /** For a range, whether the count is to its start, to its end, or since its end. */
    val phase: RangePhase,
    /** The day the count refers to: the occurrence, or a range's end once it has begun. */
    val date: CalendarDate,
    /** The event's time, when the count refers to its start. */
    val time: TimeOfDay?,
    /** Whether the date needs its year (it isn't in the current year). */
    val showsYear: Boolean,
    val isPinned: Boolean,
)

public data class CategoryFilter(val category: EventCategory?, val isSelected: Boolean)

/**
 * How many events lie on each side of today; the landscape's hills grow with them. Anything on
 * today's date (all day, or timed earlier or later today) counts with the future side, where the
 * today dot is.
 */
public data class LandscapeCounts(val past: Int, val future: Int) {
    public companion object {
        public fun of(counts: List<DayCount>): LandscapeCounts =
            LandscapeCounts(
                past = counts.count { it is DayCount.Past },
                future = counts.count { it !is DayCount.Past },
            )
    }
}

public enum class HomePlaceholder {
    /** No events at all: the first-date illustration and an Add button. */
    NO_EVENTS,

    /** Events exist, but none in the selected category: a Reset button. */
    NO_MATCHES,
}

/** Which change couldn't be stored. */
public enum class HomeFailure {
    PINNING,
    DELETING,
}
