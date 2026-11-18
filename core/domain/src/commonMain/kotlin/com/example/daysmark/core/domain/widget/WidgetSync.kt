package com.example.daysmark.core.domain.widget

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

/**
 * Publishes a new widget timeline whenever events or the pin change, at every midnight, and when
 * `clockChanges` emits (see [com.example.daysmark.core.domain.reminders.ReminderSync]). Every
 * publication goes through one [Reconciler].
 */
public class WidgetSync(
    private val events: EventRepository,
    private val clock: WallClock,
    private val publisher: WidgetPublisher,
    private val reporter: ErrorReporter,
) {
    private val reconciler =
        Reconciler(
            compute = {
                val snapshot = events.observeSnapshot().first()
                WidgetTimeline.of(snapshot.events, snapshot.pinnedId, clock.now(), clock.timeZone())
            },
            apply = { publisher.publish(it) },
        )

    /**
     * Publishes the current timeline now and returns when it is stored: for a time-change broadcast
     * handled without the app's screens. Throws if publishing failed.
     */
    public suspend fun publishNow() {
        reconciler.reconcile(force = true)
    }

    public fun start(scope: CoroutineScope, clockChanges: Flow<Unit> = flowOf(Unit)): Job =
        combine(events.observeSnapshot(), clock.daysAcross(clockChanges)) { _, _ -> }
            .restartingOnFailure(reporter)
            .conflate()
            .onEach { attempt(reporter, "Publishing the widget failed") { reconciler.reconcile() } }
            .launchIn(scope)
}
