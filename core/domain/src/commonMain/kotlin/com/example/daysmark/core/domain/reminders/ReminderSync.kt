package com.example.daysmark.core.domain.reminders

import com.example.daysmark.core.domain.diagnostics.ErrorReporter
import com.example.daysmark.core.domain.repository.EventRepository
import com.example.daysmark.core.domain.sync.Reconciler
import com.example.daysmark.core.domain.sync.attempt
import com.example.daysmark.core.domain.sync.restartingOnFailure
import com.example.daysmark.core.domain.time.WallClock
import com.example.daysmark.core.domain.time.daysAcross
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.TimeZone

/**
 * Keeps the system's pending notifications equal to the plan: re-plans whenever events change, at
 * every midnight, and when [start]'s `clockChanges` emits (the time or zone was changed, or the app
 * came back after the device slept through a midnight). Started once per process by the platform;
 * every scheduling goes through one [Reconciler].
 */
public class ReminderSync(
    private val events: EventRepository,
    private val clock: WallClock,
    private val scheduler: ReminderScheduler,
    private val reporter: ErrorReporter,
) {
    /**
     * The plan with the zone it was made in: a zone change leaves the wall-clock plan equal but
     * moves every alarm, so it must count as a change.
     */
    private data class Plan(val reminders: List<PlannedReminder>, val zone: TimeZone)

    private val reconciler =
        Reconciler(
            compute = {
                val zone = clock.timeZone()
                Plan(ReminderPlanner.plan(events.observeEvents().first(), clock.now(), zone), zone)
            },
            apply = { scheduler.replaceAll(it.reminders) },
        )

    /**
     * Schedules the current plan now, even if it was already scheduled (alarms are lost on reboot),
     * and returns when the system holds it: for a boot or time-change broadcast, when no screen is
     * open. Throws if scheduling failed.
     */
    public suspend fun replanNow() {
        reconciler.reconcile(force = true)
    }

    public fun start(scope: CoroutineScope, clockChanges: Flow<Unit> = flowOf(Unit)): Job =
        // The changes only say "look again"; the reconciler reads the current events itself.
        combine(events.observeEvents(), clock.daysAcross(clockChanges)) { _, _ -> }
            .restartingOnFailure(reporter)
            .conflate()
            .onEach { attempt(reporter, "Scheduling reminders failed") { reconciler.reconcile() } }
            .launchIn(scope)
}
