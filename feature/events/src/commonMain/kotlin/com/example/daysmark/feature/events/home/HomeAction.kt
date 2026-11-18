package com.example.daysmark.feature.events.home

import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventId

/** Everything a user can do on Home. The ViewModel's only input. */
public sealed interface HomeAction {
    /** `null` selects "All". */
    public data class SelectCategory(val category: EventCategory?) : HomeAction

    public data object ResetFilters : HomeAction

    /** Pins the event, or unpins it if it is the pinned one. */
    public data class TogglePin(val id: EventId) : HomeAction

    /** Sent after the user confirmed; Home asks first. */
    public data class Delete(val id: EventId) : HomeAction

    /** Reads the events again after [HomeUiState.Failed]. */
    public data object Retry : HomeAction

    /** The user saw [HomeUiState.Content.failure]. */
    public data object DismissFailure : HomeAction
}
