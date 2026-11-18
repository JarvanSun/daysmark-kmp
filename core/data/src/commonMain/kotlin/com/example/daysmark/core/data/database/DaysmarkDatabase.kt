package com.example.daysmark.core.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities =
        [
            EventEntity::class,
            ReminderEntity::class,
            PinnedEventEntity::class,
            OnboardingEntity::class,
        ],
    version = 1,
)
@ConstructedBy(DaysmarkDatabaseConstructor::class)
public abstract class DaysmarkDatabase : RoomDatabase() {
    internal abstract fun eventDao(): EventDao

    internal abstract fun pinDao(): PinDao

    internal abstract fun onboardingDao(): OnboardingDao

    internal abstract fun snapshotDao(): SnapshotDao

    internal companion object {
        const val FILE_NAME: String = "daysmark.db"
    }
}

// Room's KSP processor generates the `actual` for every target.
@Suppress("KotlinNoActualForExpect")
internal expect object DaysmarkDatabaseConstructor : RoomDatabaseConstructor<DaysmarkDatabase> {
    override fun initialize(): DaysmarkDatabase
}
