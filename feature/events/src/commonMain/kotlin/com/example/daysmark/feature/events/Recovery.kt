package com.example.daysmark.feature.events

import com.example.daysmark.core.domain.diagnostics.ErrorReporter
import com.example.daysmark.core.domain.repository.StorageException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update

/**
 * A screen's stored data, read again on request. Stored data that can't be read
 * ([StorageException]) is reported and becomes the screen's failed state instead of ending the
 * flow, and [retry] starts it again (docs/spec/presentation.md, "Failures"). Anything else is a
 * programmer error and is not caught.
 */
internal class Retryable(private val reporter: ErrorReporter) {
    private val attempts = MutableStateFlow(0)

    fun retry() {
        attempts.update { it + 1 }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> read(failed: T, source: () -> Flow<T>): Flow<T> = attempts.flatMapLatest {
        source().catch { cause ->
            if (cause !is StorageException) throw cause
            reporter.report(cause, "A screen couldn't read its data")
            emit(failed)
        }
    }
}

/**
 * Runs a write the user asked for and returns whether it was stored. A [StorageException] is
 * reported and becomes `false`, for the caller to show; anything else propagates.
 */
internal suspend fun ErrorReporter.succeeded(write: suspend () -> Unit): Boolean =
    try {
        write()
        true
    } catch (failure: StorageException) {
        report(failure, "A change couldn't be stored")
        false
    }
