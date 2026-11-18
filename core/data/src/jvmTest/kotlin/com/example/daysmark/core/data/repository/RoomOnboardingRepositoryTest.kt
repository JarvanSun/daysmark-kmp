package com.example.daysmark.core.data.repository

import com.example.daysmark.core.data.database.buildDaysmarkDatabase
import com.example.daysmark.core.data.database.inMemoryDatabaseBuilder
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.Reminder
import com.example.daysmark.core.domain.model.TimeOfDay
import com.example.daysmark.core.testing.FixedWallClock
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class RoomOnboardingRepositoryTest {
    private val database = inMemoryDatabaseBuilder().buildDaysmarkDatabase()
    private val onboarding =
        RoomOnboardingRepository(database, FixedWallClock(LocalDate(2026, 11, 18)))
    private val events = RoomEventRepository(database)

    @AfterTest fun closeDatabase() = database.close()

    @Test
    fun aNewDatabaseHasNotBeenOnboarded() = runTest {
        assertFalse(onboarding.observeIsCompleted().first())
    }

    @Test
    fun completingSavesTheStartersWithTheirRemindersAndMarksItDone() = runTest {
        val birthday =
            Event(
                EventId("starter-birthday"),
                "My Birthday",
                CalendarDate(1996, 3, 3),
                reminders = listOf(Reminder(7, TimeOfDay.MORNING)),
            )

        onboarding.complete(listOf(birthday))

        assertTrue(onboarding.observeIsCompleted().first())
        assertEquals(listOf(birthday), events.observeEvents().first())
    }

    @Test
    fun completingWithNothingStillMarksItDone() = runTest {
        onboarding.complete(emptyList())

        assertTrue(onboarding.observeIsCompleted().first())
        assertEquals(emptyList(), events.observeEvents().first())
    }
}
