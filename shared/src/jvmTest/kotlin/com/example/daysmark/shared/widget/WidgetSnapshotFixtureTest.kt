package com.example.daysmark.shared.widget

import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.calculation.RangePhase
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.widget.WidgetEntry
import com.example.daysmark.core.domain.widget.WidgetFocus
import com.example.daysmark.core.domain.widget.WidgetTimeline
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.serialization.json.Json

/**
 * The widget format against resources/fixtures/widget-timeline-v2.json, which the Swift widget's
 * tests decode too: Kotlin must write exactly what Swift reads.
 */
class WidgetSnapshotFixtureTest {
    private val fixture =
        File(System.getProperty("daysmark.fixtures"), "widget-timeline-v2.json").readText()

    private fun entry(day: Int, focus: WidgetFocus?) =
        WidgetEntry(Instant.fromEpochMilliseconds(1_795_017_600_000 + day * 86_400_000L), focus)

    private fun focus(id: String, title: String, icon: String, count: DayCount, phase: RangePhase) =
        WidgetFocus(EventId(id), title, icon, count, phase)

    private val timeline =
        WidgetTimeline(
            listOf(
                entry(
                    0,
                    focus("trip", "Kyoto", "airplane", DayCount.Future(3), RangePhase.BEFORE_START),
                ),
                entry(1, focus("trip", "Kyoto", "airplane", DayCount.Today, RangePhase.ONGOING)),
                entry(
                    2,
                    focus(
                        "dinner",
                        "Dinner",
                        "star",
                        DayCount.Hours(2, isFuture = false),
                        RangePhase.POINT,
                    ),
                ),
                entry(
                    3,
                    focus(
                        "call",
                        "Call",
                        "heart",
                        DayCount.Minutes(5, isFuture = true),
                        RangePhase.POINT,
                    ),
                ),
                entry(4, focus("call", "Call", "heart", DayCount.Now, RangePhase.POINT)),
                entry(5, focus("trip", "Kyoto", "airplane", DayCount.Past(12), RangePhase.ENDED)),
                entry(6, null),
            )
        )

    @Test
    fun encodesExactlyTheSharedFixture() {
        assertEquals(
            Json.parseToJsonElement(fixture),
            Json.parseToJsonElement(WidgetSnapshot.encode(timeline)),
        )
    }

    @Test
    fun decodesTheSharedFixture() {
        assertEquals(timeline, WidgetSnapshot.decode(fixture))
    }

    @Test
    fun rejectsEveryInvalidSharedFixture() {
        val invalid =
            File(System.getProperty("daysmark.fixtures"), "widget-timeline-invalid").listFiles()
        assertEquals(3, invalid?.size)
        invalid.orEmpty().forEach { file ->
            assertEquals(null, WidgetSnapshot.decode(file.readText()), file.name)
        }
    }
}
