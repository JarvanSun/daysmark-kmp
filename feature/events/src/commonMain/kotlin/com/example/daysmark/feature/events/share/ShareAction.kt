package com.example.daysmark.feature.events.share

/** What the user can change on the share screen before sharing. */
public sealed interface ShareAction {
    public data class SetIncludesCategory(val includes: Boolean) : ShareAction

    /** Reads the event again after [ShareUiState.Failed]. */
    public data object Retry : ShareAction
}
