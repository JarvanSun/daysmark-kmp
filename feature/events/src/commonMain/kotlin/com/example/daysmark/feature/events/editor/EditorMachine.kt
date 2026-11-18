package com.example.daysmark.feature.events.editor

/**
 * Where the editor is. Each [EditorEvent] is valid only in some phases; [on] is the whole state
 * machine, pure, so every transition is tested directly. The ViewModel performs the writes that
 * entering [Saving] and [Deleting] call for, and feeds their results back as events.
 */
internal sealed interface EditorPhase {
    data object Loading : EditorPhase

    data object LoadFailed : EditorPhase

    /**
     * The user is editing; [failure] is the last write that couldn't be stored. A new event
     * ([isNew]) can't be deleted: there is nothing stored yet.
     */
    data class Editing(
        val draft: EventDraft,
        val original: EventDraft,
        val isNew: Boolean,
        val failure: EditorFailure? = null,
    ) : EditorPhase

    /** Writing [draft]; edits and a second save or delete are ignored until it finishes. */
    data class Saving(val draft: EventDraft, val original: EventDraft, val isNew: Boolean) :
        EditorPhase

    data class Deleting(val draft: EventDraft, val original: EventDraft) : EditorPhase

    /** Saved, deleted, or deleted elsewhere: the platform closes the editor. Final. */
    data object Finished : EditorPhase

    fun on(event: EditorEvent): EditorPhase =
        when (this) {
            Loading,
            LoadFailed -> loading(event)
            is Editing -> editing(event)
            is Saving -> busy(event, Editing(draft, original, isNew, EditorFailure.SAVING))
            is Deleting ->
                busy(event, Editing(draft, original, isNew = false, EditorFailure.DELETING))
            Finished -> this
        }

    private fun loading(event: EditorEvent): EditorPhase =
        when (event) {
            is EditorEvent.Loaded -> Editing(event.draft, event.original, event.isNew)
            EditorEvent.LoadFailed -> LoadFailed
            EditorEvent.Retry -> Loading
            EditorEvent.Gone -> Finished
            else -> this
        }

    private fun Editing.editing(event: EditorEvent): EditorPhase =
        when (event) {
            is EditorEvent.Edit -> copy(draft = DraftEditing.apply(draft, event.action))
            EditorEvent.Save -> if (draft.title.isBlank()) this else Saving(draft, original, isNew)
            EditorEvent.Delete -> if (isNew) this else Deleting(draft, original)
            EditorEvent.DismissFailure -> copy(failure = null)
            EditorEvent.Gone -> Finished
            else -> this
        }

    /** A write is running: only its outcome, or the event disappearing, moves on. */
    private fun busy(event: EditorEvent, afterFailure: Editing): EditorPhase =
        when (event) {
            EditorEvent.WriteSucceeded,
            EditorEvent.Gone -> Finished
            EditorEvent.WriteFailed -> afterFailure
            else -> this
        }
}

/** What can happen to the editor: the user's actions and the outcomes of reads and writes. */
internal sealed interface EditorEvent {
    /** The draft to edit; [original] is what is stored (or was, when a session is restored). */
    data class Loaded(
        val draft: EventDraft,
        val original: EventDraft = draft,
        val isNew: Boolean = false,
    ) : EditorEvent

    /** The first read failed; once loaded, a failing watch reconnects without an event. */
    data object LoadFailed : EditorEvent

    data object Retry : EditorEvent

    data class Edit(val action: EditorAction) : EditorEvent

    data object Save : EditorEvent

    data object Delete : EditorEvent

    data object WriteSucceeded : EditorEvent

    data object WriteFailed : EditorEvent

    data object DismissFailure : EditorEvent

    /** The event no longer exists (deleted on another screen, or the update found nothing). */
    data object Gone : EditorEvent
}
