package com.example.daysmark.core.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/** Reads that must see one version of several tables. */
@Dao
internal interface SnapshotDao {
    @Transaction @Query("SELECT * FROM events") suspend fun events(): List<EventWithReminders>

    @Query("SELECT eventId FROM pinned_event WHERE slot = 0") suspend fun pinnedId(): String?

    /** The events and the pin from the same transaction. */
    @Transaction
    suspend fun read(): Pair<List<EventWithReminders>, String?> = events() to pinnedId()
}
