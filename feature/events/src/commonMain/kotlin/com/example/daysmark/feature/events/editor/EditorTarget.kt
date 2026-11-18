package com.example.daysmark.feature.events.editor

import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventId

/** What the editor opens on: a new event in a category, or an existing event. */
public sealed interface EditorTarget {
    public data class New(val category: EventCategory) : EditorTarget

    public data class Existing(val id: EventId) : EditorTarget

    /** An editing session saved by the platform (Android, across process death), resumed as is. */
    public data class Resume(val session: EditorSession) : EditorTarget
}

/**
 * Everything needed to resume an editor exactly where it was: which event (a new one keeps the id
 * it was given), the draft and what is stored. The platform saves it from
 * [EditorUiState.Editing.session] and passes it back through [EditorTarget.Resume].
 */
public data class EditorSession(
    val eventId: EventId,
    val isNew: Boolean,
    val draft: EventDraft,
    val original: EventDraft,
)
