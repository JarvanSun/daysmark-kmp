package com.example.daysmark.core.domain.sync

import com.example.daysmark.core.domain.diagnostics.ErrorReporter
import com.example.daysmark.core.domain.repository.StorageException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen

/**
 * Keeps a process-long pipeline alive: if its source can't be read ([StorageException]), it is
 * reported and starts again after a growing pause, 1 s up to [longest]. Any other failure is a
 * programmer error and ends the pipeline; cancellation too.
 */
public fun <T> Flow<T>.restartingOnFailure(
    reporter: ErrorReporter,
    longest: Duration = 1.minutes,
): Flow<T> = retryWhen { cause, attempt ->
    if (cause !is StorageException) return@retryWhen false
    reporter.report(cause, "Reading for a sync failed; trying again")
    val doublings = min(attempt, MAX_DOUBLINGS).toInt()
    delay(minOf(1.seconds * (1 shl doublings), longest))
    true
}

/**
 * Runs one reconciliation of a sync and returns its failure, or `null`. A failure is reported and
 * not fatal: the next look tries again. Every exception counts, because a platform's refusal
 * (scheduling, writing a file) arrives as whatever type the platform throws; reporting keeps a
 * programmer error visible. Cancellation is rethrown.
 */
public suspend fun attempt(
    reporter: ErrorReporter,
    context: String,
    effect: suspend () -> Unit,
): Exception? =
    try {
        effect()
        null
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
        reporter.report(failure, context)
        failure
    }

private const val MAX_DOUBLINGS = 6L
