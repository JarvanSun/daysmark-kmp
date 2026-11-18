package com.example.daysmark.android.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/**
 * The colour treatment iOS gives the artwork (`.saturation`, `.contrast`, `.brightness`,
 * `.colorMultiply`, applied in that order), as one colour matrix, so both platforms tone the same
 * files the same way.
 */
fun artworkFilter(
    saturation: Float = 1f,
    contrast: Float = 1f,
    brightness: Float = 0f,
    multiply: Color? = null,
    screenOver: Color? = null,
): ColorFilter {
    var matrix = saturationMatrix(saturation)
    matrix = then(matrix, affine(scale = contrast, offset = (1 - contrast) * HALF + brightness))
    multiply?.let { matrix = then(matrix, scaleMatrix(it.red, it.green, it.blue)) }
    screenOver?.let { matrix = then(matrix, screenMatrix(it)) }
    return ColorFilter.colorMatrix(ColorMatrix(matrix))
}

/**
 * The "multiply" blend of an illustration drawn on a flat [page] colour, as a filter: on a flat
 * colour, multiplying with the page equals tinting by it. Deterministic, unlike layer blending.
 */
fun multiplyOver(page: Color, saturation: Float = 1f): ColorFilter =
    artworkFilter(saturation = saturation, multiply = page)

/** The "screen" blend on a flat [page] colour: s + p − s·p, an affine map per channel. */
private fun screenMatrix(page: Color): FloatArray =
    floatArrayOf(
        1 - page.red,
        0f,
        0f,
        0f,
        page.red * CHANNEL_MAX,
        0f,
        1 - page.green,
        0f,
        0f,
        page.green * CHANNEL_MAX,
        0f,
        0f,
        1 - page.blue,
        0f,
        page.blue * CHANNEL_MAX,
        0f,
        0f,
        0f,
        1f,
        0f,
    )

private const val HALF = 0.5f
private const val CHANNEL_MAX = 255f
private const val LUMA_R = 0.2126f
private const val LUMA_G = 0.7152f
private const val LUMA_B = 0.0722f

/** Rows of 5 (r, g, b, a, offset); offsets in 0–255 like Android's ColorMatrix. */
private fun saturationMatrix(s: Float): FloatArray {
    val r = (1 - s) * LUMA_R
    val g = (1 - s) * LUMA_G
    val b = (1 - s) * LUMA_B
    return floatArrayOf(
        r + s,
        g,
        b,
        0f,
        0f,
        r,
        g + s,
        b,
        0f,
        0f,
        r,
        g,
        b + s,
        0f,
        0f,
        0f,
        0f,
        0f,
        1f,
        0f,
    )
}

private fun affine(scale: Float, offset: Float): FloatArray =
    floatArrayOf(
        scale,
        0f,
        0f,
        0f,
        offset * CHANNEL_MAX,
        0f,
        scale,
        0f,
        0f,
        offset * CHANNEL_MAX,
        0f,
        0f,
        scale,
        0f,
        offset * CHANNEL_MAX,
        0f,
        0f,
        0f,
        1f,
        0f,
    )

private fun scaleMatrix(r: Float, g: Float, b: Float): FloatArray =
    floatArrayOf(
        r,
        0f,
        0f,
        0f,
        0f,
        0f,
        g,
        0f,
        0f,
        0f,
        0f,
        0f,
        b,
        0f,
        0f,
        0f,
        0f,
        0f,
        1f,
        0f,
    )

private const val ROWS = 4
private const val COLUMNS = 5
private const val OFFSET = 4

/** [first] applied, then [next]: next × first, as 4×5 affine colour matrices. */
private fun then(first: FloatArray, next: FloatArray): FloatArray {
    val out = FloatArray(ROWS * COLUMNS)
    for (row in 0 until ROWS) {
        for (col in 0 until COLUMNS) {
            var sum = if (col == OFFSET) next[row * COLUMNS + OFFSET] else 0f
            for (k in 0 until ROWS) sum += next[row * COLUMNS + k] * first[k * COLUMNS + col]
            out[row * COLUMNS + col] = sum
        }
    }
    return out
}
