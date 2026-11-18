package com.example.daysmark.feature.events.detail

import com.example.daysmark.core.domain.calculation.CountedEvent
import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.diagnostics.ErrorReporter
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.repository.EventRepository
import com.example.daysmark.core.domain.time.WallClock
import com.example.daysmark.core.domain.time.seconds
import com.example.daysmark.feature.events.Retryable
import com.example.daysmark.feature.events.WhileOnScreen
import com.example.daysmark.feature.events.succeeded
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.stateIn
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

/** One event's detail: the large count, the ticking line and the pin (docs/spec/product.md). */
@AssistedInject
public class DetailViewModel(
    @Assisted private val eventId: EventId,
    private val events: EventRepository,
    private val clock: WallClock,
    private val reporter: ErrorReporter,
) : ViewModel() {
    /** How many times the user cycled the unit; [content] maps it onto today's units. */
    private val cycles = MutableStateFlow(0)

    private val pinFailed = MutableStateFlow(false)
    private val reads = Retryable(reporter)

    @NativeCoroutinesState
    public val uiState: StateFlow<DetailUiState> =
        reads
            .read<DetailUiState>(failed = DetailUiState.Failed) {
                combine(
                    events.observeEvent(eventId),
                    events.observePinnedEventId(),
                    clock.seconds(),
                    cycles,
                    pinFailed,
                ) { event, pinned, now, cycles, failed ->
                    if (event == null) DetailUiState.Missing
                    else
                        content(event, event.id == pinned, now, clock.timeZone(), cycles)
                            .copy(pinFailed = failed)
                }
            }
            .stateIn(
                viewModelScope,
                WhileOnScreen,
                DetailUiState.Loading,
            )

    public fun onAction(action: DetailAction) {
        when (action) {
            // One atomic step on the ViewModel's own state, never a read of the derived uiState:
            // two quick taps are always two steps.
            DetailAction.CycleUnit -> cycles.update { it + 1 }
            DetailAction.TogglePin ->
                viewModelScope.coroutineScope.launch {
                    if (!reporter.succeeded { events.togglePin(eventId) }) pinFailed.value = true
                }
            DetailAction.Retry -> reads.retry()
            DetailAction.DismissFailure -> pinFailed.value = false
        }
    }

    @AssistedFactory
    public fun interface Factory {
        public fun create(eventId: EventId): DetailViewModel
    }
}

/** The detail content at one moment; pure, so it is tested directly. */
internal fun content(
    event: Event,
    isPinned: Boolean,
    now: Instant,
    zone: TimeZone,
    cycles: Int,
): DetailUiState.Content {
    val counted = CountedEvent.of(event, now, zone)
    val time = event.time.takeIf { counted.target == counted.occurrence }
    val moment = counted.target.atTime(time?.toLocalTime() ?: LocalTime(0, 0)).toInstant(zone)
    // On the day itself (all day, or the timed moment) the count is just "Today".
    val isTheMoment = counted.dayCount == DayCount.Today || counted.dayCount == DayCount.Now
    val units = unitsFor(direction(counted.dayCount, moment - now), counted.dayCount)
    return DetailUiState.Content(
        id = event.id,
        title = event.title,
        icon = event.icon,
        date = CalendarDate.of(counted.target),
        time = time,
        dayCount = counted.dayCount,
        phase = counted.phase,
        isPinned = isPinned,
        count =
            detailCount(
                counted.dayCount,
                moment - now,
                units[cycles % units.size],
            ),
        ticker = if (counted.dayCount == DayCount.Today) null else elapsed(moment - now),
        canSwitchUnit = units.size > 1,
        isTheDay = counted.dayCount.isToday,
    )
}

private const val DAYS_PER_WEEK = 7
private const val HOURS_PER_DAY = 24
private const val MINUTES_PER_HOUR = 60
private const val SECONDS_PER_MINUTE = 60

private fun detailCount(dayCount: DayCount, untilMoment: Duration, unit: DetailUnit): DetailCount {
    val days =
        when (dayCount) {
            is DayCount.Future -> dayCount.days
            is DayCount.Past -> dayCount.days
            else -> 0
        }
    val direction = direction(dayCount, untilMoment)
    val magnitude = untilMoment.absoluteValue
    return when (unit) {
        DetailUnit.DAYS -> DetailCount(days.toLong(), unit, direction, 0)
        DetailUnit.WEEKS ->
            DetailCount((days / DAYS_PER_WEEK).toLong(), unit, direction, days % DAYS_PER_WEEK)
        DetailUnit.HOURS ->
            DetailCount(rounded(magnitude, 1.hours, untilMoment.isPositive()), unit, direction, 0)
        DetailUnit.MINUTES ->
            DetailCount(rounded(magnitude, 1.minutes, untilMoment.isPositive()), unit, direction, 0)
    }
}

/**
 * Whole [unit]s, rounded like every phrase (docs/spec/product.md): up while ahead, so 1 h 57 m to
 * go reads "2 hours until" as Home's "in 2 hours" does; down once past.
 */
private fun rounded(magnitude: Duration, unit: Duration, isAhead: Boolean): Long {
    val whole = magnitude.inWholeMilliseconds / unit.inWholeMilliseconds
    val hasRest = magnitude.inWholeMilliseconds % unit.inWholeMilliseconds != 0L
    return if (isAhead && hasRest) whole + 1 else whole
}

private fun direction(dayCount: DayCount, untilMoment: Duration): CountDirection =
    when {
        dayCount is DayCount.Future -> CountDirection.UPCOMING
        dayCount is DayCount.Past -> CountDirection.PAST
        dayCount == DayCount.Today || dayCount == DayCount.Now -> CountDirection.TODAY
        untilMoment.isPositive() -> CountDirection.UPCOMING
        else -> CountDirection.PAST
    }

/**
 * The units the large number can be shown in. On the day itself only "Today"; earlier or later on a
 * timed event's day only hours and minutes, since its days and weeks are zero ("0 days" is never
 * shown, docs/spec/product.md).
 */
internal fun unitsFor(direction: CountDirection, dayCount: DayCount): List<DetailUnit> =
    when {
        direction == CountDirection.TODAY -> listOf(DetailUnit.DAYS)
        dayCount.isToday -> listOf(DetailUnit.HOURS, DetailUnit.MINUTES)
        else -> DetailUnit.entries
    }

private fun elapsed(untilMoment: Duration): Elapsed {
    val total = abs(untilMoment.inWholeSeconds)
    val minutes = total / SECONDS_PER_MINUTE
    val hours = minutes / MINUTES_PER_HOUR
    return Elapsed(
        days = hours / HOURS_PER_DAY,
        hours = (hours % HOURS_PER_DAY).toInt(),
        minutes = (minutes % MINUTES_PER_HOUR).toInt(),
        seconds = (total % SECONDS_PER_MINUTE).toInt(),
    )
}
