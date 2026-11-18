package com.example.daysmark.feature.events.share

import com.example.daysmark.core.domain.calculation.CountedEvent
import com.example.daysmark.core.domain.diagnostics.ErrorReporter
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.TimeOfDay
import com.example.daysmark.core.domain.repository.EventRepository
import com.example.daysmark.core.domain.time.WallClock
import com.example.daysmark.feature.events.Retryable
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.stateIn
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.toLocalDateTime

/** The share screen for one event: a static card and the system share sheet. */
@AssistedInject
public class ShareViewModel(
    @Assisted eventId: EventId,
    events: EventRepository,
    clock: WallClock,
    private val reporter: ErrorReporter,
) : ViewModel() {
    private val includesCategory = MutableStateFlow(false)
    private val asOf = clock.now()
    private val reads = Retryable(reporter)

    @NativeCoroutinesState
    public val uiState: StateFlow<ShareUiState> =
        reads
            .read<ShareUiState>(failed = ShareUiState.Failed) {
                combine(events.observeEvent(eventId), includesCategory) { event, includes ->
                    if (event == null) return@combine ShareUiState.Missing
                    val zone = clock.timeZone()
                    val counted = CountedEvent.of(event, asOf, zone)
                    val moment = asOf.toLocalDateTime(zone)
                    ShareUiState.Content(
                        title = event.title,
                        icon = event.icon,
                        dayCount = counted.dayCount,
                        phase = counted.phase,
                        start = CalendarDate.of(counted.occurrence),
                        end = counted.occurrenceEnd?.let(CalendarDate::of),
                        time = event.time,
                        category = event.category,
                        includesCategory = includes,
                        asOfDate = CalendarDate.of(moment.date),
                        asOfTime = TimeOfDay(moment.hour, moment.minute),
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ShareUiState.Loading)

    public fun onAction(action: ShareAction) {
        when (action) {
            is ShareAction.SetIncludesCategory -> includesCategory.value = action.includes
            ShareAction.Retry -> reads.retry()
        }
    }

    @AssistedFactory
    public fun interface Factory {
        public fun create(eventId: EventId): ShareViewModel
    }
}
