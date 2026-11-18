package com.example.daysmark.core.testing

import com.example.daysmark.core.domain.diagnostics.ErrorReporter

/** Keeps every reported failure, so a test can check that a failure was reported, not hidden. */
public class RecordingErrorReporter : ErrorReporter {
    public val reported: MutableList<Pair<Throwable, String>> = mutableListOf()

    override fun report(failure: Throwable, context: String) {
        reported += failure to context
    }
}
