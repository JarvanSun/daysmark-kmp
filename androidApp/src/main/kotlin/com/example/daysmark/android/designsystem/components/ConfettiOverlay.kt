package com.example.daysmark.android.designsystem.components

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import com.example.daysmark.feature.events.detail.Confetti
import kotlin.random.Random

private const val NANOS_PER_SECOND = 1_000_000_000.0
private const val HUE_DEGREES = 360f

/** Pieces are [side] wide and half as tall, centred on their position. */
private const val HEIGHT_RATIO = 0.5f

/** Confetti that falls once on the event's day. Positions come from the shared [Confetti]. */
@Composable
fun ConfettiOverlay() {
    val context = LocalContext.current
    // Respect "Remove animations": no confetti when the user turned animations off.
    val animationsOff =
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    if (animationsOff) return
    val confetti = remember { Confetti(seed = Random.nextInt()) }
    var elapsedNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (elapsedNanos / NANOS_PER_SECOND < confetti.lifetimeSeconds) {
            withFrameNanos { elapsedNanos = it - start }
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val density = density
        confetti
            .piecesAt(
                elapsedNanos / NANOS_PER_SECOND,
                size.width / density.toDouble(),
                size.height / density.toDouble(),
            )
            .forEach { piece ->
                val side = (piece.side * density).toFloat()
                translate(
                    (piece.centerX * density).toFloat(),
                    (piece.centerY * density).toFloat(),
                ) {
                    rotate(piece.rotationDegrees.toFloat(), pivot = Offset.Zero) {
                        drawRect(
                            color =
                                Color.hsv(
                                    (piece.hue * HUE_DEGREES).toFloat(),
                                    Confetti.SATURATION.toFloat(),
                                    1f,
                                ),
                            topLeft = Offset(-side / 2, -side * HEIGHT_RATIO / 2),
                            size = Size(side, side * HEIGHT_RATIO),
                        )
                    }
                }
            }
    }
}
