@file:OptIn(ExperimentalForeignApi::class)

package com.example.daysmark.shared

import com.example.daysmark.core.data.database.databaseBuilder
import com.example.daysmark.core.domain.diagnostics.ErrorReporter
import com.example.daysmark.core.domain.reminders.ReminderScheduler
import com.example.daysmark.core.domain.widget.WidgetPublisher
import dev.zacsweers.metro.createGraphFactory
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** iOS's entry point; called once from the App. */
public fun createAppGraph(
    reminderScheduler: ReminderScheduler,
    widgetPublisher: WidgetPublisher,
    errorReporter: ErrorReporter,
): AppGraph =
    createGraphFactory<MetroAppGraph.Factory>()
        .create(
            databaseBuilder(applicationSupportPath()),
            SystemWallClock,
            reminderScheduler,
            widgetPublisher,
            errorReporter,
        )

private fun applicationSupportPath(): String {
    val url =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
    return requireNotNull(url?.path) { "Application Support directory is unavailable" }
}
