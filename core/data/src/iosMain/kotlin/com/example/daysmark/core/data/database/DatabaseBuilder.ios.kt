package com.example.daysmark.core.data.database

import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The database file inside [directoryPath], the app's Application Support directory. Only the app
 * opens it; the widget reads a timeline file in the App Group (docs/spec/product.md).
 */
public fun databaseBuilder(directoryPath: String): RoomDatabase.Builder<DaysmarkDatabase> =
    Room.databaseBuilder<DaysmarkDatabase>(name = "$directoryPath/${DaysmarkDatabase.FILE_NAME}")
