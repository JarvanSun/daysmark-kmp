package com.example.daysmark.feature.events.detail

/** What the detail screen can change itself; editing and sharing are navigation. */
public sealed interface DetailAction {
    /** Days → weeks → hours → minutes → days. */
    public data object CycleUnit : DetailAction

    public data object TogglePin : DetailAction

    /** Reads the event again after [DetailUiState.Failed]. */
    public data object Retry : DetailAction

    /** The user saw [DetailUiState.Content.pinFailed]. */
    public data object DismissFailure : DetailAction
}
