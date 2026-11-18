package com.example.daysmark.core.data.database

import androidx.room.Room
import androidx.room.RoomDatabase

/** An in-memory database: the JVM target exists only for fast tests. */
public fun inMemoryDatabaseBuilder(): RoomDatabase.Builder<DaysmarkDatabase> =
    Room.inMemoryDatabaseBuilder<DaysmarkDatabase>()
