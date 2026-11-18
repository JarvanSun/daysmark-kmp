package com.example.daysmark.shared

import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.reminders.PlannedReminder
import com.example.daysmark.core.domain.reminders.ReminderScheduler
import com.example.daysmark.core.domain.reminders.ReminderSync
import com.example.daysmark.core.domain.widget.WidgetSync
import com.example.daysmark.core.domain.widget.WidgetTimeline
import com.example.daysmark.core.testing.FakeEventRepository
import com.example.daysmark.core.testing.FixedWallClock
import com.example.daysmark.core.testing.RecordingErrorReporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

/**
 * The service's lifetime, observed through its effects: how many reads are live and how often the
 * widget was published, on a scheduler the test advances.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppServicesTest {
    private val repository = FakeEventRepository()
    private val clock = FixedWallClock(LocalDate(2026, 11, 18))
    private val published = mutableListOf<WidgetTimeline>()

    private object NoScheduler : ReminderScheduler {
        override suspend fun replaceAll(reminders: List<PlannedReminder>) = Unit
    }

    private fun TestScope.services(): Pair<AppServices, CoroutineScope> {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val reporter = RecordingErrorReporter()
        return AppServices(
            ReminderSync(repository, clock, NoScheduler, reporter),
            WidgetSync(repository, clock, { published += it }, reporter),
            reporter,
            scope,
        ) to scope
    }

    /** Each sync keeps one read open: the reminders' events, the widget's snapshot. */
    private val oneSetOfReads = 2

    @Test
    fun startingTwiceRunsOneSetOfSyncs() = runTest {
        val (services, scope) = services()
        services.start()
        services.start()
        runCurrent()

        assertEquals(oneSetOfReads, repository.activeReaders)
        assertEquals(1, published.size)
        scope.coroutineContext.cancelChildren()
    }

    @Test
    fun startRestartsSyncsThatStoppedAndTheyWorkAgain() = runTest {
        val (services, scope) = services()
        services.start()
        runCurrent()
        scope.coroutineContext.cancelChildren()
        runCurrent()
        assertEquals(0, repository.activeReaders)

        services.start()
        repository.events.value = listOf(Event(EventId("trip"), "Kyoto", CalendarDate(2026, 12, 1)))
        runCurrent()

        assertEquals(oneSetOfReads, repository.activeReaders)
        assertEquals("trip", published.last().entries.first().focus?.id?.value)
        scope.coroutineContext.cancelChildren()
    }
}
