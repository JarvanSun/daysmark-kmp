package com.example.daysmark.core.data.repository

import com.example.daysmark.core.domain.repository.StorageException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * The data layer's boundary: whatever Room, SQLite or the mapping of a stored row throws leaves as
 * a [StorageException] carrying it as the cause. Callers above see one failure type for "the stored
 * data couldn't be used", and every other exception stays a programmer error.
 */
internal suspend fun <T> storage(what: String, block: suspend () -> T): T =
    try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
        throw StorageException("Couldn't $what", failure)
    }

/** [storage] for a stored data stream. */
internal fun <T> Flow<T>.storage(what: String): Flow<T> = catch { failure ->
    if (failure is CancellationException) throw failure
    throw StorageException("Couldn't $what", failure)
}
