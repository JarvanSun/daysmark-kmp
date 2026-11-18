package com.example.daysmark.core.data.repository

import androidx.room.execSQL
import androidx.room.useWriterConnection
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
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

/**
 * The "all or nothing" promises, tested by making a write fail halfway: a trigger aborts one
 * insert, and everything else in the same transaction must be rolled back. Without the transactions
 * these tests fail, which the success-path tests can't show.
 */
class TransactionRollbackTest {
    private val database = inMemoryDatabaseBuilder().buildDaysmarkDatabase()
    private val events = RoomEventRepository(database)
    private val onboarding =
        RoomOnboardingRepository(database, FixedWallClock(LocalDate(2026, 11, 18)))

    @AfterTest fun closeDatabase() = database.close()

    private suspend fun failInsertsInto(table: String) = database.useWriterConnection {
        it.execSQL(
            "CREATE TRIGGER fail_$table BEFORE INSERT ON $table " +
                "BEGIN SELECT RAISE(ABORT, 'disk full'); END"
        )
    }

    private val trip =
        Event(
            EventId("trip"),
            "Kyoto",
            CalendarDate(2026, 12, 1),
            reminders = listOf(Reminder(1, TimeOfDay.MORNING)),
        )

    @Test
    fun onboardingThatCantMarkItselfDoneSavesNoStarters() = runTest {
        failInsertsInto("onboarding")

        assertFails { onboarding.complete(listOf(trip)) }

        assertEquals(emptyList(), events.observeEvents().first())
        assertFalse(onboarding.observeIsCompleted().first())
    }

    @Test
    fun addingAnEventWhoseRemindersCantBeStoredStoresNothing() = runTest {
        failInsertsInto("reminders")

        assertFails { events.add(trip) }

        assertEquals(emptyList(), events.observeEvents().first())
    }

    @Test
    fun anUpdateWhoseRemindersCantBeStoredKeepsTheOldEventAndReminders() = runTest {
        events.add(trip)
        failInsertsInto("reminders")

        assertFails {
            events.update(
                trip.copy(
                    title = "Kyoto in winter",
                    reminders = listOf(Reminder(7, TimeOfDay(20, 0))),
                )
            )
        }

        assertEquals(listOf(trip), events.observeEvents().first())
    }
}
