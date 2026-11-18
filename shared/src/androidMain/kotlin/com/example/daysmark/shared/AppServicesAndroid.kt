package com.example.daysmark.shared

import com.example.daysmark.core.domain.sync.attempt

/**
 * Plans reminders and publishes the widget once, returning when both are stored, for the boot and
 * time-change receivers, which run without the app's screens and may be the process's only work.
 * One failing doesn't stop the other (each implementation logs its failure). Android only: iOS
 * keeps scheduled notifications across reboots by itself.
 */
public suspend fun AppServices.replanAll() {
    attempt(reporter, "Scheduling reminders after a time change failed") { reminders.replanNow() }
    attempt(reporter, "Publishing the widget after a time change failed") { widget.publishNow() }
    onClockChanged()
}
