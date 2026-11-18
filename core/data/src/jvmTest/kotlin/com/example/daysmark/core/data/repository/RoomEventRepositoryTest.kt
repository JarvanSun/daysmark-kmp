package com.example.daysmark.core.data.repository

import com.example.daysmark.core.data.database.buildDaysmarkDatabase
import com.example.daysmark.core.data.database.inMemoryDatabaseBuilder
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventIcon
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.Reminder
import com.example.daysmark.core.domain.model.RepeatRule
import com.example.daysmark.core.domain.model.TimeOfDay
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class RoomEventRepositoryTest {
    private val database = inMemoryDatabaseBuilder().buildDaysmarkDatabase()
    private val repository = RoomEventRepository(database)

    @AfterTest fun closeDatabase() = database.close()

    private fun event(id: String, title: String = id) =
        Event(EventId(id), title, CalendarDate(2027, 1, 1))

    @Test
    fun savedEventRoundTripsWithEveryField() = runTest {
        val event =
            Event(
                id = EventId("leap-day"),
                title = "Leap Day",
                date = CalendarDate(2028, 2, 29),
                time = TimeOfDay(19, 30),
                endDate = CalendarDate(2028, 3, 2),
                reminders = listOf(Reminder(1, TimeOfDay.MORNING), Reminder(0, TimeOfDay(18, 0))),
                repeat = RepeatRule.YEARLY,
                category = EventCategory.ANNIVERSARY,
                icon = EventIcon.BIRTHDAY_CAKE,
            )

        repository.add(event)

        assertEquals(listOf(event), repository.observeEvents().first())
    }

    @Test
    fun updatingReplacesTheEvent() = runTest {
        repository.add(event("a", "Old"))

        assertTrue(repository.update(event("a", "New")))
        assertEquals(listOf("New"), repository.observeEvents().first().map { it.title })
    }

    @Test
    fun updatingADeletedEventChangesNothingAndSaysSo() = runTest {
        repository.add(event("a"))
        repository.delete(EventId("a"))

        assertFalse(repository.update(event("a", "Late edit")))
        assertEquals(emptyList(), repository.observeEvents().first())
    }

    @Test
    fun addingAnExistingIdFails() = runTest {
        repository.add(event("a"))
        assertFails { repository.add(event("a", "Again")) }
    }

    @Test
    fun pinningReplacesThePreviousPin() = runTest {
        repository.add(event("a"))
        repository.add(event("b"))
        repository.togglePin(EventId("a"))
        repository.togglePin(EventId("b"))

        assertEquals(EventId("b"), repository.observePinnedEventId().first())
    }

    @Test
    fun togglingThePinnedEventUnpinsIt() = runTest {
        repository.add(event("a"))
        repository.togglePin(EventId("a"))
        repository.togglePin(EventId("a"))

        assertNull(repository.observePinnedEventId().first())
    }

    @Test
    fun anEventThatIsntStoredCantBePinned() = runTest {
        repository.togglePin(EventId("gone"))

        assertNull(repository.observePinnedEventId().first())
    }

    @Test
    fun deletingThePinnedEventAlsoUnpinsIt() = runTest {
        repository.add(event("a"))
        repository.togglePin(EventId("a"))

        repository.delete(EventId("a"))

        assertEquals(emptyList(), repository.observeEvents().first())
        assertNull(repository.observePinnedEventId().first())
    }

    @Test
    fun savingAgainReplacesTheReminders() = runTest {
        val event = event("a").copy(reminders = listOf(Reminder(1, TimeOfDay.MORNING)))
        repository.add(event)
        repository.update(event.copy(reminders = listOf(Reminder(7, TimeOfDay(20, 0)))))

        assertEquals(
            listOf(Reminder(7, TimeOfDay(20, 0))),
            repository.observeEvent(EventId("a")).first()?.reminders,
        )
    }

    @Test
    fun aDeletedEventIsObservedAsGone() = runTest {
        repository.add(event("a").copy(reminders = listOf(Reminder(1, TimeOfDay.MORNING))))
        repository.delete(EventId("a"))

        assertNull(repository.observeEvent(EventId("a")).first())
    }

    @Test
    fun deletingAnotherEventKeepsThePin() = runTest {
        repository.add(event("a"))
        repository.add(event("b"))
        repository.togglePin(EventId("a"))

        repository.delete(EventId("b"))

        assertEquals(EventId("a"), repository.observePinnedEventId().first())
    }

    @Test
    fun theSnapshotHasTheEventsAndThePinTogether() = runTest {
        repository.add(event("a"))
        repository.togglePin(EventId("a"))

        val snapshot = repository.observeSnapshot().first()

        assertEquals(listOf("a"), snapshot.events.map { it.id.value })
        assertEquals(EventId("a"), snapshot.pinnedId)
    }
}
