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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * An [EditorSession] as a short string, for Android's saved instance state, so an editor survives
 * the process being killed in the background. The domain types stay free of serialization; this
 * private mirror is the only format, and an unreadable string restores nothing (`null`).
 */
public object EditorSessionCodec {
    private val json = Json { ignoreUnknownKeys = false }

    public fun encode(session: EditorSession): String =
        json.encodeToString(Saved.serializer(), Saved.of(session))

    public fun decode(text: String): EditorSession? = runCatching {
        json.decodeFromString(Saved.serializer(), text).toSession()
    }
        .getOrNull()

    @Serializable
    private data class Saved(
        val id: String,
        val isNew: Boolean,
        val draft: Draft,
        val original: Draft,
    ) {
        fun toSession() = EditorSession(EventId(id), isNew, draft.toDraft(), original.toDraft())

        companion object {
            fun of(session: EditorSession) =
                Saved(
                    session.eventId.value,
                    session.isNew,
                    Draft.of(session.draft),
                    Draft.of(session.original),
                )
        }
    }

    /** Dates as [year, month, day], times as [hour, minute], enums by their storage keys. */
    @Serializable
    private data class Draft(
        val title: String,
        val date: List<Int>,
        val time: List<Int>?,
        val endDate: List<Int>?,
        val repeat: String,
        val category: String,
        val icon: String,
        val reminders: List<List<Int>>,
    ) {
        fun toDraft() =
            EventDraft(
                title = title,
                date = date.toDate(),
                time = time?.toTime(),
                endDate = endDate?.toDate(),
                repeat = RepeatRule.fromStorageKey(repeat),
                category = EventCategory.fromStorageKey(category),
                icon = EventIcon.fromStorageKey(icon),
                reminders =
                    reminders.map { (days, hour, minute) ->
                        Reminder(days, TimeOfDay(hour, minute))
                    },
            )

        companion object {
            fun of(draft: EventDraft) =
                Draft(
                    title = draft.title,
                    date = draft.date.toList(),
                    time = draft.time?.toList(),
                    endDate = draft.endDate?.toList(),
                    repeat = draft.repeat.storageKey,
                    category = draft.category.storageKey,
                    icon = draft.icon.storageKey,
                    reminders =
                        draft.reminders.map { listOf(it.daysBefore, it.time.hour, it.time.minute) },
                )
        }
    }

    private fun CalendarDate.toList() = listOf(year, month, day)

    private fun List<Int>.toDate() = CalendarDate(this[0], this[1], this[2])

    private fun TimeOfDay.toList() = listOf(hour, minute)

    private fun List<Int>.toTime() = TimeOfDay(this[0], this[1])
}
