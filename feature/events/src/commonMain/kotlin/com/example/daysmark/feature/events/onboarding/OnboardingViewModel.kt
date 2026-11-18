package com.example.daysmark.feature.events.onboarding

import com.example.daysmark.core.domain.diagnostics.ErrorReporter
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.onboarding.StarterEvent
import com.example.daysmark.core.domain.repository.OnboardingRepository
import com.example.daysmark.core.domain.time.WallClock
import com.example.daysmark.core.domain.time.days
import com.example.daysmark.core.domain.time.today
import com.example.daysmark.feature.events.WhileOnScreen
import com.example.daysmark.feature.events.succeeded
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.stateIn
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The first-run introduction. Every step can be skipped; the only permission involved is
 * notifications, which the platform asks for when the birthday starter (with its reminders) is
 * created ([OnboardingUiState.finishAsksForNotifications], docs/spec/product.md). Finishing saves
 * the chosen starters and the "done" mark together; the launch gate ([LaunchViewModel]) then shows
 * Home.
 */
@Inject
public class OnboardingViewModel(
    private val onboarding: OnboardingRepository,
    private val clock: WallClock,
    private val reporter: ErrorReporter,
) : ViewModel() {
    private val progress = MutableStateFlow(OnboardingProgress(birthday = defaultBirthday()))

    @NativeCoroutinesState
    public val uiState: StateFlow<OnboardingUiState> =
        combine(progress, clock.days()) { progress, _ ->
                progress.toUiState(clock.now(), clock.timeZone())
            }
            .stateIn(
                viewModelScope,
                WhileOnScreen,
                progress.value.toUiState(clock.now(), clock.timeZone()),
            )

    public fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.Continue -> progress.update { it.next() }
            OnboardingAction.Back -> progress.update { it.previous() }
            OnboardingAction.SkipBirthday ->
                progress.update {
                    it.copy(step = OnboardingStep.STARTERS, keepsBirthday = false)
                }
            is OnboardingAction.SetBirthday ->
                progress.update { it.copy(birthday = action.date, choseBirthday = true) }
            is OnboardingAction.Toggle -> progress.update { it.toggle(action.starter) }
            is OnboardingAction.Finish -> finish(action.titles)
            OnboardingAction.StartEmpty -> finish(titles = null)
            OnboardingAction.RetrySave -> lastRequest?.let(::save)
            OnboardingAction.DismissSaveFailure -> progress.update { it.copy(saveFailed = false) }
        }
    }

    /** Saves the selected starters (none when [titles] is `null`) and marks onboarding done. */
    private fun finish(titles: Map<StarterEvent, String>?) {
        val current = progress.value
        if (current.isSaving) return
        val today = clock.today()
        val birthday = current.birthday.takeIf { current.keepsBirthday }
        val events =
            titles
                ?.let { names ->
                    current
                        .offered()
                        .filter { it in current.selection }
                        .mapNotNull { starter ->
                            starter.event(names[starter].orEmpty(), today, birthday)
                        }
                }
                .orEmpty()
        save(events)
    }

    /** What the last finish tried to save, so a retry repeats it rather than re-deciding. */
    private var lastRequest: List<Event>? = null

    private fun save(events: List<Event>) {
        if (progress.value.isSaving) return
        lastRequest = events
        progress.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.coroutineScope.launch {
            // A storage failure: tell the user and let them try again. On success the launch gate
            // replaces this screen; staying "saving" until then keeps a second tap from saving.
            if (!reporter.succeeded { onboarding.complete(events) }) {
                progress.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    /** 1 January, [DEFAULT_AGE_YEARS] years ago: only where the picker starts. */
    private fun defaultBirthday(): CalendarDate =
        CalendarDate(clock.today().year - DEFAULT_AGE_YEARS, 1, 1)

    private companion object {
        /** Where the birthday picker starts; any date works as a starting point. */
        const val DEFAULT_AGE_YEARS = 25
    }
}
