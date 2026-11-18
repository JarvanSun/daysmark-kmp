package com.example.daysmark.core.testing

import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.repository.EventRepository
import com.example.daysmark.core.domain.repository.EventSnapshot
import com.example.daysmark.core.domain.repository.StorageException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * An in-memory [EventRepository]; tests set [events] and [pinned] directly and read them back.
 *
 * Failures and timing: set [failReads] to make reads fail with a [StorageException], including ones
 * already being collected (a database failing mid-stream); [writeFailure] to make the next write
 * throw; [writeGate] to hold writes until it completes.
 */
public class FakeEventRepository : EventRepository {
    public val events: MutableStateFlow<List<Event>> = MutableStateFlow(emptyList())
    public val pinned: MutableStateFlow<EventId?> = MutableStateFlow(null)
    private val readFailures = MutableStateFlow(false)
    public var failReads: Boolean
        get() = readFailures.value
        set(value) {
            readFailures.value = value
        }

    public var writeFailure: Exception? = null
    public var writeGate: CompletableDeferred<Unit>? = null
    public var writeCount: Int = 0

    /** How many reads are being collected right now: shows a screen's queries stopping. */
    public var activeReaders: Int = 0
        private set

    override fun observeEvents(): Flow<List<Event>> = readable(events)

    override fun observeEvent(id: EventId): Flow<Event?> =
        readable(events.map { all -> all.firstOrNull { it.id == id } })

    override fun observePinnedEventId(): Flow<EventId?> = readable(pinned)

    override fun observeSnapshot(): Flow<EventSnapshot> =
        readable(combine(events, pinned) { all, pin -> EventSnapshot(all, pin) })

    override suspend fun add(event: Event): Unit = write {
        check(events.value.none { it.id == event.id }) { "${event.id} already exists" }
        events.value = events.value + event
    }

    override suspend fun update(event: Event): Boolean = write {
        if (events.value.none { it.id == event.id }) return@write false
        events.value = events.value.map { if (it.id == event.id) event else it }
        true
    }

    override suspend fun delete(id: EventId): Unit = write {
        events.value = events.value.filterNot { it.id == id }
        if (pinned.value == id) pinned.value = null
    }

    override suspend fun togglePin(id: EventId): Unit = write {
        if (events.value.none { it.id == id }) return@write
        pinned.value = if (pinned.value == id) null else id
    }

    private fun <T> readable(source: Flow<T>): Flow<T> = flow {
        activeReaders++
        try {
            emitAll(
                combine(source, readFailures) { value, failing ->
                    if (failing) throw StorageException("The database couldn't be read")
                    value
                }
            )
        } finally {
            activeReaders--
        }
    }

    private suspend fun <T> write(change: () -> T): T {
        writeCount++
        writeGate?.await()
        writeFailure?.let {
            writeFailure = null
            throw it
        }
        return change()
    }
}
