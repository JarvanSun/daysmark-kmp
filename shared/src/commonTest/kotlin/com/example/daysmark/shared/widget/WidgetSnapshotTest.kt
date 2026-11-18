package com.example.daysmark.shared.widget

import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.calculation.RangePhase
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.widget.WidgetEntry
import com.example.daysmark.core.domain.widget.WidgetFocus
import com.example.daysmark.core.domain.widget.WidgetTimeline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class WidgetSnapshotTest {
    /** The exact bytes the Swift widget's Codable types are written against. */
    @Test
    fun encodesTheSchemaTheSwiftWidgetDecodes() {
        val timeline =
            WidgetTimeline(
                listOf(
                    WidgetEntry(
                        Instant.fromEpochMilliseconds(1_000),
                        WidgetFocus(
                            EventId("trip"),
                            "Kyoto",
                            "airplane",
                            DayCount.Future(3),
                            RangePhase.BEFORE_START,
                        ),
                    ),
                    WidgetEntry(Instant.fromEpochMilliseconds(2_000), null),
                )
            )

        assertEquals(
            """{"version":2,"entries":[{"startsAtMillis":1000,"focus":{"id":"trip","title":"Kyoto",""" +
                """"icon":"airplane","count":{"kind":"future","value":3,"future":true},""" +
                """"phase":"beforeStart"}},{"startsAtMillis":2000,"focus":null}]}""",
            WidgetSnapshot.encode(timeline),
        )
    }

    @Test
    fun decodingGivesBackTheTimeline() {
        val timeline =
            WidgetTimeline(
                listOf(
                    WidgetEntry(
                        Instant.fromEpochMilliseconds(5_000),
                        WidgetFocus(
                            EventId("d"),
                            "Dinner",
                            "star",
                            DayCount.Hours(3, isFuture = false),
                            RangePhase.POINT,
                        ),
                    )
                )
            )
        assertEquals(timeline, WidgetSnapshot.decode(WidgetSnapshot.encode(timeline)))
    }

    @Test
    fun anUnreadableOrOldFileIsIgnored() {
        assertEquals(null, WidgetSnapshot.decode("not json"))
        assertEquals(null, WidgetSnapshot.decode("""{"version":0,"entries":[]}"""))
        assertEquals(
            null,
            WidgetSnapshot.decode(
                """{"version":2,"entries":[{"startsAtMillis":1,"focus":{"id":"x"}}]}"""
            ),
        )
    }
}
