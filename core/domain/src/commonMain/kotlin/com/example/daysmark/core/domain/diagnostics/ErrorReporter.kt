package com.example.daysmark.core.domain.diagnostics

/**
 * Where failures the app recovers from are recorded, with their cause, so a retry loop or a
 * "couldn't load" screen never hides why. One implementation per platform (the system log).
 */
public fun interface ErrorReporter {
    public fun report(failure: Throwable, context: String)
}
