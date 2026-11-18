package com.example.daysmark.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/** The app's database file in its private data directory. */
public fun databaseBuilder(context: Context): RoomDatabase.Builder<DaysmarkDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<DaysmarkDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(DaysmarkDatabase.FILE_NAME).absolutePath,
    )
}
