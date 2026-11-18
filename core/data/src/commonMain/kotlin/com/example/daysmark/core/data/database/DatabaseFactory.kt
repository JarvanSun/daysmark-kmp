package com.example.daysmark.core.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** The one place the database is configured; platforms only decide where the file lives. */
internal fun RoomDatabase.Builder<DaysmarkDatabase>.buildDaysmarkDatabase(): DaysmarkDatabase =
    setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
