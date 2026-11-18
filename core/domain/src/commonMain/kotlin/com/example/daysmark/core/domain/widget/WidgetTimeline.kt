package com.example.daysmark.core.domain.widget

import com.example.daysmark.core.domain.calculation.CountedEvent
import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.calculation.EventOrdering
import com.example.daysmark.core.domain.calculation.RangePhase
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventId
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * What the Home Screen widget shows over the next days: the focused event now and at each of the
 * next [DAYS] midnights. Computed by the app with the same rules as Home, so the widget (which has
 * no database and no shared code) only displays it.
 */
public data class WidgetTimeline(val entries: List<WidgetEntry>) {
    public companion object {
        /** A month: the widget stays right that long even if the app isn't opened. */
        public const val DAYS: Int = 30

        public fun of(
            events: List<Event>,
            pinned: EventId?,
            now: Instant,
            zone: TimeZone,
        ): WidgetTimeline {
            val today = now.toLocalDateTime(zone).date
            val moments =
                listOf(now) +
                    (1..DAYS).map { today.plus(DatePeriod(days = it)).atStartOfDayIn(zone) }
            return WidgetTimeline(
                moments.map { moment ->
                    val counted = events.map { CountedEvent.of(it, moment, zone) }
                    WidgetEntry(moment, EventOrdering.focus(counted, pinned)?.toFocus())
                }
            )
        }

        private fun CountedEvent.toFocus() =
            WidgetFocus(
                id = event.id,
                title = event.title,
                iconKey = event.icon.storageKey,
                dayCount = dayCount.atDayLevel(),
                phase = phase,
            )

        /**
         * The widget refreshes only at midnight, so "in 3 hours" would go stale within the hour:
         * anything happening today reads as Today there.
         */
        private fun DayCount.atDayLevel(): DayCount =
            when (this) {
                is DayCount.Hours,
                is DayCount.Minutes,
                DayCount.Now -> DayCount.Today
                else -> this
            }
    }
}

/** From [startsAt] until the next entry, the widget shows [focus] (or invites a first event). */
public data class WidgetEntry(val startsAt: Instant, val focus: WidgetFocus?)

public data class WidgetFocus(
    val id: EventId,
    val title: String,
    val iconKey: String,
    val dayCount: DayCount,
    val phase: RangePhase,
)

/** Hands the timeline to the platform's widget; one implementation per platform. */
public fun interface WidgetPublisher {
    /**
     * Stores [timeline] where the widget reads it and asks the system to redraw. Returns once it is
     * stored; throws if it couldn't be (the implementation logs why). Called one at a time.
     */
    @Throws(Exception::class) public suspend fun publish(timeline: WidgetTimeline)
}
