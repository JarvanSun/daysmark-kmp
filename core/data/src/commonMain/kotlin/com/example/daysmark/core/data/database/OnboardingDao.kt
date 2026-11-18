package com.example.daysmark.core.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface OnboardingDao {
    @Query("SELECT EXISTS(SELECT 1 FROM onboarding WHERE slot = 0)")
    fun observeIsCompleted(): Flow<Boolean>

    @Upsert suspend fun markCompleted(state: OnboardingEntity)
}
