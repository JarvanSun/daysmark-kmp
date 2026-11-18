package com.example.daysmark.android

import android.util.Log
import com.example.daysmark.core.domain.diagnostics.ErrorReporter

/** Records failures the app recovers from in the system log, with their cause. */
class LogErrorReporter : ErrorReporter {
    override fun report(failure: Throwable, context: String) {
        Log.w("Daysmark", context, failure)
    }
}
