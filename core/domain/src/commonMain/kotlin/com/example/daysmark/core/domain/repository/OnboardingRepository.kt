package com.example.daysmark.core.domain.repository

import com.example.daysmark.core.domain.model.Event
import kotlinx.coroutines.flow.Flow

/** Whether the first-run introduction is done, and finishing it. */
public interface OnboardingRepository {
    public fun observeIsCompleted(): Flow<Boolean>

    /** Saves [events] and marks onboarding done, together: either both happen or neither. */
    public suspend fun complete(events: List<Event>)
}
