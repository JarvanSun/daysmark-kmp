package com.example.daysmark.feature.events.home

import com.example.daysmark.core.domain.calculation.CountedEvent
import com.example.daysmark.core.domain.calculation.EventOrdering
import com.example.daysmark.core.domain.diagnostics.ErrorReporter
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.repository.EventRepository
import com.example.daysmark.core.domain.time.WallClock
import com.example.daysmark.core.domain.time.minutes
import com.example.daysmark.feature.events.Retryable
import com.example.daysmark.feature.events.WhileOnScreen
import com.example.daysmark.feature.events.succeeded
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.stateIn
import dev.zacsweers.metro.Inject
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Home: the focused event, the landscape, category filters and the list. Unidirectional: [uiState]
 * out, [onAction] in (docs/spec/presentation.md). The state is recomputed every minute, so "in 3
 * hours" counts down and "Tomorrow" becomes "Today" without a reload.
 */
@Inject
public class HomeViewModel(
    private val events: EventRepository,
    private val clock: WallClock,
    private val reporter: ErrorReporter,
) : ViewModel() {
    private val selectedCategory = MutableStateFlow<EventCategory?>(null)
    private val failure = MutableStateFlow<HomeFailure?>(null)
    private val reads = Retryable(reporter)

    @NativeCoroutinesState
    public val uiState: StateFlow<HomeUiState> =
        reads
            .read<HomeUiState>(failed = HomeUiState.Failed) {
                // One snapshot, so the focus never pairs a pin with a list it wasn't stored with.
                combine(events.observeSnapshot(), clock.minutes(), selectedCategory, failure) {
                    snapshot,
                    now,
                    category,
                    failed ->
                    content(snapshot.events, snapshot.pinnedId, now, clock.timeZone(), category)
                        .copy(failure = failed)
                }
            }
            .stateIn(
                viewModelScope,
                WhileOnScreen,
                HomeUiState.Loading,
            )

    public fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.SelectCategory -> selectedCategory.value = action.category
            HomeAction.ResetFilters -> selectedCategory.value = null
            is HomeAction.TogglePin -> write(HomeFailure.PINNING) { events.togglePin(action.id) }
            is HomeAction.Delete -> write(HomeFailure.DELETING) { events.delete(action.id) }
            HomeAction.Retry -> reads.retry()
            HomeAction.DismissFailure -> failure.value = null
        }
    }

    private fun write(kind: HomeFailure, change: suspend () -> Unit) {
        viewModelScope.coroutineScope.launch {
            if (!reporter.succeeded(change)) failure.value = kind
        }
    }

    private companion object {
        fun content(
            all: List<Event>,
            pinned: EventId?,
            now: Instant,
            zone: TimeZone,
            category: EventCategory?,
        ): HomeUiState.Content {
            val today = now.toLocalDateTime(zone).date
            val counted =
                all.map { CountedEvent.of(it, now, zone) }.sortedWith(EventOrdering.listOrder)
            val focus = EventOrdering.focus(counted, pinned)
            val shown = counted.filter { category == null || it.event.category == category }
            return HomeUiState.Content(
                focus = focus?.toRow(today, pinned),
                landscape = LandscapeCounts.of(counted.map { it.dayCount }),
                filters =
                    listOf(CategoryFilter(null, category == null)) +
                        EventCategory.entries.map { CategoryFilter(it, it == category) },
                rows = shown.map { it.toRow(today, pinned) },
                placeholder =
                    when {
                        counted.isEmpty() -> HomePlaceholder.NO_EVENTS
                        shown.isEmpty() -> HomePlaceholder.NO_MATCHES
                        else -> null
                    },
            )
        }

        fun CountedEvent.toRow(today: LocalDate, pinned: EventId?) =
            EventRow(
                id = event.id,
                title = event.title,
                icon = event.icon,
                category = event.category,
                dayCount = dayCount,
                phase = phase,
                date = CalendarDate.of(target),
                time = event.time.takeIf { target == occurrence },
                showsYear = target.year != today.year,
                isPinned = event.id == pinned,
            )
    }
}
