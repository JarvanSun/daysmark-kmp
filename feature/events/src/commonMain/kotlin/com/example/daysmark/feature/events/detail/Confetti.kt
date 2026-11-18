package com.example.daysmark.feature.events.detail

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * The confetti that falls once on an event's day. Where every piece is at a moment is computed
 * here, so SwiftUI and Compose draw the same fall; they only paint the rectangles.
 *
 * Sizes are in the platform's layout unit (points, dp).
 */
public class Confetti(seed: Int = 0) {
    private val pieces: List<Piece> =
        Random(seed).let { random ->
            List(PIECE_COUNT) {
                Piece(
                    x = random.nextDouble(),
                    speed = random.nextDouble(MIN_SPEED, MAX_SPEED),
                    spin = random.nextDouble(-MAX_SPIN, MAX_SPIN),
                    hue = random.nextDouble(),
                    size = random.nextDouble(),
                    delay = random.nextDouble(0.0, MAX_DELAY),
                )
            }
        }

    /** When the last piece has left the screen. */
    public val lifetimeSeconds: Double = pieces.maxOf { it.delay + EXIT_TIME / it.speed }

    /** Every visible piece [elapsedSeconds] after the start, in a [width] × [height] area. */
    public fun piecesAt(
        elapsedSeconds: Double,
        width: Double,
        height: Double,
    ): List<PiecePosition> = pieces.mapNotNull { piece ->
        val time = elapsedSeconds - piece.delay
        val y = -ENTRY_OFFSET + time * piece.speed * height
        if (time <= 0 || y >= height + ENTRY_OFFSET) return@mapNotNull null
        PiecePosition(
            centerX = piece.x * width + sin(time * 2 + piece.x * SWAY_PHASE) * SWAY,
            centerY = y,
            rotationDegrees = time * piece.spin * DEGREES_PER_RADIAN,
            side = MIN_SIDE + (MAX_SIDE - MIN_SIDE) * piece.size,
            hue = piece.hue,
        )
    }

    /**
     * A rectangle [side] wide and half as tall; colour hue 0–1 at 0.75 saturation, full brightness.
     */
    public data class PiecePosition(
        val centerX: Double,
        val centerY: Double,
        val rotationDegrees: Double,
        val side: Double,
        val hue: Double,
    )

    private data class Piece(
        val x: Double,
        val speed: Double,
        val spin: Double,
        val hue: Double,
        val size: Double,
        val delay: Double,
    )

    public companion object {
        public const val SATURATION: Double = 0.75
        private const val PIECE_COUNT = 220
        private const val MIN_SPEED = 0.16
        private const val MAX_SPEED = 0.42
        private const val MAX_SPIN = 6.0
        private const val MAX_DELAY = 2.2
        private const val EXIT_TIME = 1.1
        private const val ENTRY_OFFSET = 20.0
        private const val SWAY = 22.0
        private const val SWAY_PHASE = 9.0
        private const val MIN_SIDE = 6.0
        private const val MAX_SIDE = 13.0
        private const val DEGREES_PER_RADIAN = 180 / PI
    }
}
