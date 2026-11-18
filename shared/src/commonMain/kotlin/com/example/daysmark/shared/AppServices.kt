package com.example.daysmark.shared

import com.example.daysmark.core.domain.diagnostics.ErrorReporter
import com.example.daysmark.core.domain.reminders.ReminderSync
import com.example.daysmark.core.domain.widget.WidgetSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Work that runs for the whole process, started by the platform at launch: keeping notifications
 * and the widget in step with the events. Runs off the main thread ([scope] defaults to
 * [Dispatchers.Default]); the platform implementations move to their own threads where a system API
 * needs it.
 */
public class AppServices
internal constructor(
    internal val reminders: ReminderSync,
    internal val widget: WidgetSync,
    internal val reporter: ErrorReporter,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val clockChanges = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    private var jobs: List<Job> = emptyList()

    /**
     * Starts both syncs. Calling it while they run does nothing; if a sync's job has stopped (its
     * coroutines were cancelled while the scope lives on), both are started again. A cancelled
     * scope is the end of the process's services and is not revived. Call from the main thread.
     */
    public fun start() {
        if (jobs.isNotEmpty() && jobs.all { it.isActive }) return
        jobs.forEach { it.cancel() }
        jobs = listOf(reminders.start(scope, clockChanges), widget.start(scope, clockChanges))
    }

    /**
     * The system clock or time zone changed, or the app came back to the foreground (the device may
     * have slept through a midnight): plan reminders and the widget again from the new clock.
     */
    public fun onClockChanged() {
        clockChanges.tryEmit(Unit)
    }
}
