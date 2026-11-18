package com.example.daysmark.shared

import com.example.daysmark.core.domain.diagnostics.ErrorReporter
import com.example.daysmark.core.domain.reminders.ReminderScheduler
import com.example.daysmark.core.domain.reminders.ReminderSync
import com.example.daysmark.core.domain.repository.EventRepository
import com.example.daysmark.core.domain.time.WallClock
import com.example.daysmark.core.domain.widget.WidgetPublisher
import com.example.daysmark.core.domain.widget.WidgetSync
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/** Domain services have no DI annotations (docs/spec/architecture.md); they are built here. */
@BindingContainer
@ContributesTo(AppScope::class)
internal object SharedBindings {
    @Provides
    @SingleIn(AppScope::class)
    private fun provideReminderSync(
        events: EventRepository,
        clock: WallClock,
        scheduler: ReminderScheduler,
        reporter: ErrorReporter,
    ): ReminderSync = ReminderSync(events, clock, scheduler, reporter)

    @Provides
    @SingleIn(AppScope::class)
    private fun provideWidgetSync(
        events: EventRepository,
        clock: WallClock,
        publisher: WidgetPublisher,
        reporter: ErrorReporter,
    ): WidgetSync = WidgetSync(events, clock, publisher, reporter)

    @Provides
    @SingleIn(AppScope::class)
    private fun provideServices(
        reminders: ReminderSync,
        widget: WidgetSync,
        reporter: ErrorReporter,
    ): AppServices = AppServices(reminders, widget, reporter)
}
