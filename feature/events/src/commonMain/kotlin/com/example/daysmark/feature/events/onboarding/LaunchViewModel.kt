package com.example.daysmark.feature.events.onboarding

import com.example.daysmark.core.domain.diagnostics.ErrorReporter
import com.example.daysmark.core.domain.repository.OnboardingRepository
import com.example.daysmark.feature.events.Retryable
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.stateIn
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

public enum class LaunchDestination {
    /** The flag isn't read yet: show the app's background, nothing else. */
    LOADING,
    ONBOARDING,
    HOME,

    /** The flag couldn't be read; the platform offers [LaunchAction.Retry]. */
    FAILED,
}

public sealed interface LaunchAction {
    /** Reads the onboarding flag again after [LaunchDestination.FAILED]. */
    public data object Retry : LaunchAction
}

/** Decides what the app opens to: onboarding on first run, Home ever after. */
@Inject
public class LaunchViewModel(
    onboarding: OnboardingRepository,
    private val reporter: ErrorReporter,
) : ViewModel() {
    private val reads = Retryable(reporter)

    @NativeCoroutinesState
    public val uiState: StateFlow<LaunchDestination> =
        reads
            .read(failed = LaunchDestination.FAILED) {
                onboarding.observeIsCompleted().map {
                    if (it) LaunchDestination.HOME else LaunchDestination.ONBOARDING
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, LaunchDestination.LOADING)

    public fun onAction(action: LaunchAction) {
        when (action) {
            LaunchAction.Retry -> reads.retry()
        }
    }
}
