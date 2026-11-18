package com.example.daysmark.core.domain.repository

import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventId
import kotlinx.coroutines.flow.Flow

/** Stored events and the one pinned event. Implemented in `:core:data`. */
public interface EventRepository {
    public fun observeEvents(): Flow<List<Event>>

    /** One event, or `null` once it is deleted. */
    public fun observeEvent(id: EventId): Flow<Event?>

    /** The pinned event's ID, or `null` when Home shows the nearest event instead. */
    public fun observePinnedEventId(): Flow<EventId?>

    /**
     * The events and the pin together, read in one transaction, so a consumer never sees a pin and
     * a list that didn't exist at the same time.
     */
    public fun observeSnapshot(): Flow<EventSnapshot>

    /** Stores a new event with its reminders. Throws if an event with its id already exists. */
    public suspend fun add(event: Event)

    /**
     * Replaces a stored event and its reminders, atomically, if it still exists. Returns `false`
     * when it was deleted meanwhile, so a late edit can never bring a deleted event back.
     */
    public suspend fun update(event: Event): Boolean

    /** Deletes the event; if it was pinned, nothing is pinned afterwards. */
    public suspend fun delete(id: EventId)

    /** Pins [id] (replacing any pinned event), or unpins it if it is the pinned one. Atomic. */
    public suspend fun togglePin(id: EventId)
}

/** All events and the pinned one's ID, as stored at one moment. */
public data class EventSnapshot(val events: List<Event>, val pinnedId: EventId?)
