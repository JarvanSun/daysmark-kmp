package com.example.daysmark.shared.widget

import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.calculation.RangePhase
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.widget.WidgetEntry
import com.example.daysmark.core.domain.widget.WidgetFocus
import com.example.daysmark.core.domain.widget.WidgetTimeline
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The file the widgets read: a versioned, platform-neutral JSON form of [WidgetTimeline]. The Swift
 * widget decodes the same schema with Codable (`iosApp/DaysmarkWidget/WidgetSnapshot.swift`), so
 * changing a field here means changing it there and raising [VERSION]. Counts are encoded in full
 * (hours, minutes, now) although today's timeline only produces day-level ones, so the format
 * doesn't change if a widget ever shows the time of day.
 */
public object WidgetSnapshot {
    public const val VERSION: Int = 2
    public const val FILE_NAME: String = "widget-timeline.json"

    private val json = Json

    public fun encode(timeline: WidgetTimeline): String =
        json.encodeToString(
            Snapshot.serializer(),
            Snapshot(
                VERSION,
                timeline.entries.map {
                    Entry(it.startsAt.toEpochMilliseconds(), it.focus?.toFocus())
                },
            ),
        )

    /**
     * Reads a file written by [encode]; `null` if it is missing, from another version or damaged.
     */
    public fun decode(text: String): WidgetTimeline? {
        // Parsing and mapping both stay inside: a damaged file must give `null`, never throw.
        return runCatching {
            json
                .decodeFromString(Snapshot.serializer(), text)
                .takeIf { it.version == VERSION }
                ?.let {
                    WidgetTimeline(
                        it.entries.map { entry ->
                            WidgetEntry(
                                Instant.fromEpochMilliseconds(entry.startsAtMillis),
                                entry.focus?.toDomain(),
                            )
                        }
                    )
                }
        }
            .getOrNull()
    }

    @Serializable internal data class Snapshot(val version: Int, val entries: List<Entry>)

    @Serializable internal data class Entry(val startsAtMillis: Long, val focus: Focus?)

    @Serializable
    internal data class Focus(
        val id: String,
        val title: String,
        val icon: String,
        val count: Count,
        val phase: String,
    )

    /**
     * [kind] is today, now, future, past, hours or minutes; [value] the amount (0 when there is
     * none). Every field is required and an unknown kind or phase makes the file invalid: the
     * format is strict on both sides (resources/fixtures/widget-timeline-invalid).
     */
    @Serializable
    internal data class Count(
        val kind: String,
        val value: Int,
        @SerialName("future") val isFuture: Boolean,
    )

    private fun WidgetFocus.toFocus() =
        Focus(
            id.value,
            title,
            iconKey,
            dayCount.toCount(),
            phase.key(),
        )

    private fun Focus.toDomain() =
        WidgetFocus(
            EventId(id),
            title,
            icon,
            count.toDomain(),
            phase(phase),
        )

    private fun Count.toDomain(): DayCount =
        when (kind) {
            "future" -> DayCount.Future(value)
            "past" -> DayCount.Past(value)
            "hours" -> DayCount.Hours(value, isFuture)
            "minutes" -> DayCount.Minutes(value, isFuture)
            "now" -> DayCount.Now
            "today" -> DayCount.Today
            else -> throw IllegalArgumentException("Unknown count kind $kind")
        }

    private fun phase(key: String): RangePhase =
        requireNotNull(RangePhase.entries.firstOrNull { it.key() == key }) { "Unknown phase $key" }

    private fun DayCount.toCount(): Count =
        when (this) {
            DayCount.Today -> Count("today", value = 0, isFuture = true)
            DayCount.Now -> Count("now", value = 0, isFuture = true)
            is DayCount.Future -> Count("future", days, isFuture = true)
            is DayCount.Past -> Count("past", days, isFuture = false)
            is DayCount.Hours -> Count("hours", hours, isFuture)
            is DayCount.Minutes -> Count("minutes", minutes, isFuture)
        }

    private fun RangePhase.key(): String =
        when (this) {
            RangePhase.POINT -> "point"
            RangePhase.BEFORE_START -> "beforeStart"
            RangePhase.ONGOING -> "ongoing"
            RangePhase.ENDED -> "ended"
        }
}
