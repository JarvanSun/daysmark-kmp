package com.example.daysmark.core.domain.sync

import com.example.daysmark.core.domain.repository.StorageException
import com.example.daysmark.core.testing.RecordingErrorReporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ResilienceTest {
    private val reporter = RecordingErrorReporter()

    @Test
    fun aFailingSourceStartsAgainAfterAGrowingPause() = runTest {
        var runs = 0
        val source = flow {
            runs++
            if (runs < 3) throw StorageException("database busy")
            emit(runs)
        }

        assertEquals(listOf(3), source.restartingOnFailure(reporter).toList())
        // Paused 1 s, then 2 s; both failures were reported.
        assertEquals(3.seconds.inWholeMilliseconds, currentTime)
        assertEquals(2, reporter.reported.size)
    }

    @Test
    fun aProgrammerErrorIsNotRetried() = runTest {
        val source = flow<Int> { error("a bug, not the database") }
        assertFailsWith<IllegalStateException> { source.restartingOnFailure(reporter).toList() }
    }

    @Test
    fun cancellationIsNeverRetried() = runTest {
        val source = flow<Int> { throw CancellationException("stopped") }
        assertFailsWith<CancellationException> { source.restartingOnFailure(reporter).toList() }
    }

    @Test
    fun attemptReturnsTheFailureAndRethrowsCancellation() = runTest {
        assertNull(attempt(reporter, "test") {})
        assertEquals("refused", attempt(reporter, "test") { error("refused") }?.message)
        assertEquals("test", reporter.reported.single().second)
        assertFailsWith<CancellationException> {
            attempt(reporter, "test") { throw CancellationException("stop") }
        }
    }
}
