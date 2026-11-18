package com.example.daysmark.core.data.repository

import com.example.daysmark.core.data.database.DaysmarkDatabase
import com.example.daysmark.core.data.database.EventDao
import com.example.daysmark.core.data.database.PinDao
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.repository.EventRepository
import com.example.daysmark.core.domain.repository.EventSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class RoomEventRepository(private val database: DaysmarkDatabase) : EventRepository {
    private val dao: EventDao = database.eventDao()
    private val pins: PinDao = database.pinDao()

    override fun observeEvents(): Flow<List<Event>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }.storage("read the events")

    override fun observeEvent(id: EventId): Flow<Event?> =
        dao.observe(id.value).map { it?.toDomain() }.distinctUntilChanged().storage("read an event")

    override fun observePinnedEventId(): Flow<EventId?> =
        pins
            .observePinnedId()
            .map { id -> id?.let(::EventId) }
            .distinctUntilChanged()
            .storage("read the pin")

    /** Re-read in one transaction whenever any of the three tables changes. */
    override fun observeSnapshot(): Flow<EventSnapshot> =
        database.invalidationTracker
            .createFlow(EVENTS, REMINDERS, PINNED_EVENT)
            .map {
                val (rows, pinned) = database.snapshotDao().read()
                EventSnapshot(rows.map { it.toDomain() }, pinned?.let(::EventId))
            }
            .storage("read the events and the pin")

    override suspend fun add(event: Event): Unit =
        storage("add an event") { dao.add(event.toEntity(), event.reminderEntities()) }

    override suspend fun update(event: Event): Boolean =
        storage("update an event") { dao.update(event.toEntity(), event.reminderEntities()) }

    override suspend fun delete(id: EventId): Unit =
        storage("delete an event") { dao.deleteEvent(id.value) }

    override suspend fun togglePin(id: EventId): Unit =
        storage("change the pin") { pins.toggle(id.value) }

    private companion object {
        const val EVENTS = "events"
        const val REMINDERS = "reminders"
        const val PINNED_EVENT = "pinned_event"
    }
}
