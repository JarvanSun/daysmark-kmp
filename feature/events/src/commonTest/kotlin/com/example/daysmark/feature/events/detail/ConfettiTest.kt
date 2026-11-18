package com.example.daysmark.feature.events.detail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfettiTest {
    private val confetti = Confetti(seed = 1)

    @Test
    fun nothingShowsBeforeTheFirstPieceAndNothingAfterTheLast() {
        assertEquals(emptyList(), confetti.piecesAt(0.0, 400.0, 800.0))
        assertEquals(emptyList(), confetti.piecesAt(confetti.lifetimeSeconds + 0.1, 400.0, 800.0))
    }

    @Test
    fun piecesFallInsideTheAreaWithTheirSizesInRange() {
        val pieces = confetti.piecesAt(2.0, 400.0, 800.0)
        assertTrue(pieces.isNotEmpty())
        assertTrue(pieces.all { it.side in 6.0..13.0 && it.centerY in -20.0..820.0 })
    }

    @Test
    fun theSameSeedAlwaysFallsTheSameWay() {
        assertEquals(
            Confetti(7).piecesAt(1.5, 300.0, 600.0),
            Confetti(7).piecesAt(1.5, 300.0, 600.0),
        )
    }
}
