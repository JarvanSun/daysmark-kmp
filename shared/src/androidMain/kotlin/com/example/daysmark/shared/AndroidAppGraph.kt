package com.example.daysmark.shared

import android.content.Context
import com.example.daysmark.core.data.database.databaseBuilder
import com.example.daysmark.core.domain.diagnostics.ErrorReporter
import com.example.daysmark.core.domain.reminders.ReminderScheduler
import com.example.daysmark.core.domain.widget.WidgetPublisher
import dev.zacsweers.metro.createGraphFactory

/** Android's entry point; called once from the Application. */
public fun createAppGraph(
    context: Context,
    reminderScheduler: ReminderScheduler,
    widgetPublisher: WidgetPublisher,
    errorReporter: ErrorReporter,
): AppGraph =
    createGraphFactory<MetroAppGraph.Factory>()
        .create(
            databaseBuilder(context),
            SystemWallClock,
            reminderScheduler,
            widgetPublisher,
            errorReporter,
        )
