package com.example.daysmark.shared.editor

import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventIcon
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.Reminder
import com.example.daysmark.core.domain.model.RepeatRule
import com.example.daysmark.core.domain.model.TimeOfDay
import com.example.daysmark.feature.events.editor.EditorSession
import com.example.daysmark.feature.events.editor.EventDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EditorSessionCodecTest {
    private val original =
        EventDraft(
            title = "Kyoto",
            date = CalendarDate(2026, 12, 28),
            time = TimeOfDay(19, 30),
            endDate = CalendarDate(2027, 1, 3),
            repeat = RepeatRule.YEARLY,
            category = EventCategory.ANNIVERSARY,
            icon = EventIcon.AIRPLANE,
            reminders = listOf(Reminder(1, TimeOfDay.MORNING), Reminder(0, TimeOfDay(18, 0))),
        )

    @Test
    fun aSessionSurvivesTheRoundTripExactly() {
        val session =
            EditorSession(
                EventId("trip"),
                isNew = false,
                original.copy(title = "Kyoto in winter"),
                original,
            )
        assertEquals(session, EditorSessionCodec.decode(EditorSessionCodec.encode(session)))
    }

    @Test
    fun anUnreadableStringRestoresNothing() {
        assertNull(EditorSessionCodec.decode("not a session"))
    }
}
