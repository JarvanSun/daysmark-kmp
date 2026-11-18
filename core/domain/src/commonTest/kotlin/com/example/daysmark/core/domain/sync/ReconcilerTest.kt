package com.example.daysmark.core.domain.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ReconcilerTest {
    private var current = 1
    private val applied = mutableListOf<Int>()
    private var failNext = false
    private var gate: CompletableDeferred<Unit>? = null

    private val reconciler =
        Reconciler(
            compute = { current },
            apply = { value ->
                gate?.await()
                if (failNext) {
                    failNext = false
                    error("refused")
                }
                applied += value
            },
        )

    @Test
    fun anUnchangedValueIsNotAppliedAgain() = runTest {
        reconciler.reconcile()
        reconciler.reconcile()
        assertEquals(listOf(1), applied)
    }

    @Test
    fun aValueThatFailedIsAppliedAgainEvenThoughItDidNotChange() = runTest {
        failNext = true
        assertFails { reconciler.reconcile() }

        reconciler.reconcile()

        assertEquals(listOf(1), applied)
    }

    @Test
    fun forcingAppliesAnUnchangedValue() = runTest {
        reconciler.reconcile()
        reconciler.reconcile(force = true)
        assertEquals(listOf(1, 1), applied)
    }

    @Test
    fun callersTakeTurnsAndTheLaterOneComputesTheNewerValue() = runTest {
        val held = CompletableDeferred<Unit>()
        gate = held
        val first = launch { reconciler.reconcile() }
        runCurrent()
        current = 2
        val second = launch { reconciler.reconcile(force = true) }
        runCurrent()
        assertEquals(emptyList(), applied) // the second waits for the first to finish

        held.complete(Unit)
        first.join()
        second.join()

        assertEquals(listOf(1, 2), applied)
    }
}
