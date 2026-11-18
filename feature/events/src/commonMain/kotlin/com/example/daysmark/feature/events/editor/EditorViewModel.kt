package com.example.daysmark.feature.events.editor

import com.example.daysmark.core.domain.calculation.CountedEvent
import com.example.daysmark.core.domain.diagnostics.ErrorReporter
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventIcon
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.RepeatRule
import com.example.daysmark.core.domain.repository.EventRepository
import com.example.daysmark.core.domain.repository.StorageException
import com.example.daysmark.core.domain.time.WallClock
import com.example.daysmark.core.domain.time.today
import com.example.daysmark.feature.events.succeeded
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.stateIn
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

/**
 * The editor for a new or an existing event ([target]). Holds a draft; nothing is stored until
 * [EditorAction.Save] (docs/spec/presentation.md). Its states and their transitions are
 * [EditorPhase]; this class only performs the reads and writes those transitions call for. The
 * state is shared eagerly: the draft must survive the screen briefly losing its subscriber.
 */
@AssistedInject
public class EditorViewModel(
    @Assisted private val target: EditorTarget,
    private val events: EventRepository,
    private val clock: WallClock,
    private val reporter: ErrorReporter,
) : ViewModel() {
    /** Chosen once, so the editor can only ever write this one event. */
    private val eventId: EventId =
        when (target) {
            is EditorTarget.Existing -> target.id
            is EditorTarget.New -> newId()
            is EditorTarget.Resume -> target.session.eventId
        }
    private val isNew =
        target is EditorTarget.New || (target is EditorTarget.Resume && target.session.isNew)
    private val phase = MutableStateFlow<EditorPhase>(EditorPhase.Loading)
    private var loading: Job? = null

    @NativeCoroutinesState
    public val uiState: StateFlow<EditorUiState> =
        phase.map(::uiState).stateIn(viewModelScope, SharingStarted.Eagerly, EditorUiState.Loading)

    init {
        load()
    }

    public fun onAction(action: EditorAction) {
        when (action) {
            EditorAction.Save -> send(EditorEvent.Save)
            EditorAction.Delete -> send(EditorEvent.Delete)
            EditorAction.DismissFailure -> send(EditorEvent.DismissFailure)
            // Reads again only if the machine accepted the retry (it was LoadFailed).
            EditorAction.Retry ->
                if (transition(EditorEvent.Retry).second == EditorPhase.Loading) load()
            else -> send(EditorEvent.Edit(action))
        }
    }

    /** Applies [event]; performs the write when it starts one. */
    private fun send(event: EditorEvent) {
        val (before, after) = transition(event)
        when {
            before !is EditorPhase.Saving && after is EditorPhase.Saving ->
                write { save(after.draft) }
            before !is EditorPhase.Deleting && after is EditorPhase.Deleting ->
                write {
                    events.delete(eventId)
                    true
                }
        }
    }

    private fun transition(event: EditorEvent): Pair<EditorPhase, EditorPhase> {
        while (true) {
            val before = phase.value
            val after = before.on(event)
            if (phase.compareAndSet(before, after)) return before to after
        }
    }

    /** New events are added; existing ones only updated if they still exist. */
    private suspend fun save(draft: EventDraft): Boolean {
        val event = draft.copy(title = draft.title.trim()).toEvent(eventId)
        return if (isNew) {
            events.add(event)
            true
        } else {
            events.update(event)
        }
    }

    /** Runs a write; `false` means the event was gone, an exception that it couldn't be stored. */
    private fun write(change: suspend () -> Boolean) {
        viewModelScope.coroutineScope.launch {
            var stillThere = true
            val stored = reporter.succeeded { stillThere = change() }
            send(
                when {
                    !stored -> EditorEvent.WriteFailed
                    stillThere -> EditorEvent.WriteSucceeded
                    else -> EditorEvent.Gone
                }
            )
        }
    }

    private fun load() {
        loading?.cancel()
        loading =
            viewModelScope.coroutineScope.launch {
                when (target) {
                    is EditorTarget.New ->
                        send(EditorEvent.Loaded(newDraft(target.category), isNew = true))
                    is EditorTarget.Resume -> {
                        val session = target.session
                        send(EditorEvent.Loaded(session.draft, session.original, session.isNew))
                        if (!session.isNew) watch()
                    }
                    is EditorTarget.Existing -> watch()
                }
            }
    }

    /**
     * Follows the stored event: its first value starts editing; its disappearance (deleted on
     * another screen) closes the editor. If the first read fails, the editor shows Failed with
     * Retry. Once loaded, a failing watch reconnects by itself with a growing pause, leaving the
     * draft as it is: the save checks again anyway ([EventRepository.update]).
     */
    private suspend fun watch() {
        var loaded = false
        events
            .observeEvent(eventId)
            .onEach { stored ->
                loaded = true
                send(if (stored == null) EditorEvent.Gone else EditorEvent.Loaded(stored.toDraft()))
            }
            .retryWhen { cause, attempt ->
                val reconnects = loaded && cause is StorageException
                if (reconnects) {
                    reporter.report(cause, "The editor lost its event; reconnecting")
                    delay(RECONNECT_PAUSE * (1 shl attempt.coerceAtMost(MAX_DOUBLINGS).toInt()))
                }
                reconnects
            }
            .catch { cause ->
                if (cause !is StorageException) throw cause
                reporter.report(cause, "The editor couldn't read its event")
                send(EditorEvent.LoadFailed)
            }
            .collect()
    }

    private fun newDraft(category: EventCategory): EventDraft =
        EventDraft(
            title = "",
            date = CalendarDate.of(clock.today()),
            time = null,
            endDate = null,
            repeat =
                if (category == EventCategory.IMPORTANT) RepeatRule.NEVER else RepeatRule.YEARLY,
            category = category,
            icon = EventIcon.CALENDAR,
            reminders = emptyList(),
        )

    private fun uiState(phase: EditorPhase): EditorUiState =
        when (phase) {
            EditorPhase.Loading -> EditorUiState.Loading
            EditorPhase.LoadFailed -> EditorUiState.Failed
            EditorPhase.Finished -> EditorUiState.Finished
            is EditorPhase.Editing ->
                editing(phase.draft, phase.original, isBusy = false, phase.failure)
            is EditorPhase.Saving -> editing(phase.draft, phase.original, isBusy = true, null)
            is EditorPhase.Deleting -> editing(phase.draft, phase.original, isBusy = true, null)
        }

    private fun editing(
        draft: EventDraft,
        original: EventDraft,
        isBusy: Boolean,
        failure: EditorFailure?,
    ): EditorUiState.Editing {
        val counted = CountedEvent.of(draft.toEvent(eventId), clock.now(), clock.timeZone())
        return EditorUiState.Editing(
            session = EditorSession(eventId, isNew, draft, original),
            draft = draft,
            isNew = isNew,
            canSave = draft.title.isNotBlank() && !isBusy,
            isBusy = isBusy,
            failure = failure,
            hasChanges = draft != original,
            preview = EditorPreview(counted.dayCount, counted.phase),
            reminderChoices = DraftEditing.REMINDER_DAYS,
        )
    }

    @AssistedFactory
    public fun interface Factory {
        public fun create(target: EditorTarget): EditorViewModel
    }

    private companion object {
        val RECONNECT_PAUSE = 1.seconds
        const val MAX_DOUBLINGS = 5L

        @OptIn(ExperimentalUuidApi::class) fun newId() = EventId(Uuid.random().toString())

        fun Event.toDraft() =
            EventDraft(title, date, time, endDate, repeat, category, icon, reminders)

        fun EventDraft.toEvent(id: EventId) =
            Event(id, title, date, repeat, category, icon, time, endDate, reminders)
    }
}
