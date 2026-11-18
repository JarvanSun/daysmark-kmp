package com.example.daysmark.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface EventDao {
    @Transaction @Query("SELECT * FROM events") fun observeAll(): Flow<List<EventWithReminders>>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :id")
    fun observe(id: String): Flow<EventWithReminders?>

    @Upsert suspend fun upsertEvent(event: EventEntity)

    @Insert suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Query("DELETE FROM reminders WHERE eventId = :eventId")
    suspend fun deleteReminders(eventId: String)

    @Insert suspend fun insertEvent(event: EventEntity)

    /** The number of rows changed: 0 when the event no longer exists. */
    @Update suspend fun updateEvent(event: EventEntity): Int

    /** Adds an event with its reminders, in one transaction; fails if the id is taken. */
    @Transaction
    suspend fun add(event: EventEntity, reminders: List<ReminderEntity>) {
        insertEvent(event)
        insertReminders(reminders)
    }

    /**
     * Replaces an existing event and its reminders, in one transaction. Returns `false`, changing
     * nothing, when the event was deleted meanwhile.
     */
    @Transaction
    suspend fun update(event: EventEntity, reminders: List<ReminderEntity>): Boolean {
        if (updateEvent(event) == 0) return false
        deleteReminders(event.id)
        insertReminders(reminders)
        return true
    }

    /**
     * Inserts or replaces an event and its reminders, in one transaction (onboarding's starters).
     */
    @Transaction
    suspend fun save(event: EventEntity, reminders: List<ReminderEntity>) {
        upsertEvent(event)
        deleteReminders(event.id)
        insertReminders(reminders)
    }

    /** Its reminders and its pin go with it: both reference the event with `ON DELETE CASCADE`. */
    @Query("DELETE FROM events WHERE id = :id") suspend fun deleteEvent(id: String)
}

/** The one pinned event (`pinned_event`, a single row). */
@Dao
internal interface PinDao {
    @Query("SELECT eventId FROM pinned_event WHERE slot = 0") fun observePinnedId(): Flow<String?>

    /** The only write of the pin, always to slot 0: "one pin" holds because nothing else writes. */
    @Query("INSERT OR REPLACE INTO pinned_event (slot, eventId) VALUES (0, :eventId)")
    suspend fun setPinned(eventId: String)

    @Query("DELETE FROM pinned_event") suspend fun clearPinned()

    @Query("SELECT eventId FROM pinned_event WHERE slot = 0") suspend fun pinnedId(): String?

    @Query("SELECT EXISTS(SELECT 1 FROM events WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    /**
     * Reads and changes the pin in one transaction, so two quick toggles can't interleave. An id
     * that isn't stored (deleted meanwhile) changes nothing.
     */
    @Transaction
    suspend fun toggle(id: String) {
        when {
            pinnedId() == id -> clearPinned()
            exists(id) -> setPinned(id)
        }
    }
}
