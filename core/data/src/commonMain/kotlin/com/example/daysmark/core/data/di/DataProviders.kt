package com.example.daysmark.core.data.di

import androidx.room.RoomDatabase
import com.example.daysmark.core.data.database.DaysmarkDatabase
import com.example.daysmark.core.data.database.buildDaysmarkDatabase
import com.example.daysmark.core.data.repository.RoomEventRepository
import com.example.daysmark.core.data.repository.RoomOnboardingRepository
import com.example.daysmark.core.domain.repository.EventRepository
import com.example.daysmark.core.domain.repository.OnboardingRepository
import com.example.daysmark.core.domain.time.WallClock
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Everything `core:data` adds to the app graph. Implementations stay `internal`, so they are bound
 * to their domain interfaces here rather than contributed from another module. The platform
 * supplies the database builder (the file's location).
 */
@BindingContainer
@ContributesTo(AppScope::class)
public object DataProviders {
    @Provides
    @SingleIn(AppScope::class)
    private fun provideDatabase(builder: RoomDatabase.Builder<DaysmarkDatabase>): DaysmarkDatabase =
        builder.buildDaysmarkDatabase()

    @Provides
    private fun provideEventRepository(database: DaysmarkDatabase): EventRepository =
        RoomEventRepository(database)

    @Provides
    private fun provideOnboardingRepository(
        database: DaysmarkDatabase,
        clock: WallClock,
    ): OnboardingRepository = RoomOnboardingRepository(database, clock)
}
