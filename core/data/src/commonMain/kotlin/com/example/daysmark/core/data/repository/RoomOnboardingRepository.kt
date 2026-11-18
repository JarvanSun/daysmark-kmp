package com.example.daysmark.core.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.example.daysmark.core.data.database.DaysmarkDatabase
import com.example.daysmark.core.data.database.OnboardingEntity
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.repository.OnboardingRepository
import com.example.daysmark.core.domain.time.WallClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

internal class RoomOnboardingRepository(
    private val database: DaysmarkDatabase,
    private val clock: WallClock,
) : OnboardingRepository {
    override fun observeIsCompleted(): Flow<Boolean> =
        database
            .onboardingDao()
            .observeIsCompleted()
            .distinctUntilChanged()
            .storage("read whether onboarding is done")

    /** The starter events and the "done" mark in one write transaction. */
    override suspend fun complete(events: List<Event>): Unit =
        storage("finish onboarding") {
            database.useWriterConnection { connection ->
                connection.immediateTransaction {
                    events.forEach {
                        database.eventDao().save(it.toEntity(), it.reminderEntities())
                    }
                    database
                        .onboardingDao()
                        .markCompleted(
                            OnboardingEntity(completedAtMillis = clock.now().toEpochMilliseconds())
                        )
                }
            }
        }
}
