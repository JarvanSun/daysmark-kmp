package com.example.daysmark.feature.events.share

import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.calculation.RangePhase
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventIcon
import com.example.daysmark.core.domain.model.TimeOfDay

/** Everything the share card shows, frozen at the moment the share screen opened. */
public sealed interface ShareUiState {
    public data object Loading : ShareUiState

    /** The event couldn't be read; the platform offers [ShareAction.Retry]. */
    public data object Failed : ShareUiState

    public data object Missing : ShareUiState

    public data class Content(
        val title: String,
        val icon: EventIcon,
        val dayCount: DayCount,
        val phase: RangePhase,
        /** The occurrence's start, and its last day for a range. */
        val start: CalendarDate,
        val end: CalendarDate?,
        val time: TimeOfDay?,
        /** Shown only when [includesCategory]. */
        val category: EventCategory,
        val includesCategory: Boolean,
        /** "As of …": the card is a snapshot, and says when it was taken. */
        val asOfDate: CalendarDate,
        val asOfTime: TimeOfDay,
    ) : ShareUiState
}
