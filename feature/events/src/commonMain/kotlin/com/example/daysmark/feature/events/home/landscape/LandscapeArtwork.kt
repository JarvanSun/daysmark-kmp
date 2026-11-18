package com.example.daysmark.feature.events.home.landscape

/**
 * The landscape's artwork files, their geometry and their toning. Hills and slopes are wide images
 * of which only a band is used; [crop] says where that band is, in source pixels. The platforms map
 * each case to the shared file named [fileName] (resources/artwork) and apply [saturation] and
 * [contrast] before tinting it with the side's ground or foliage colour, in that order.
 */
public enum class LandscapeArtwork(
    public val fileName: String,
    private val sourceWidth: Double,
    private val sourceVisibleWidth: Double,
    private val sourceHeight: Double,
    private val cropTop: Double,
    private val cropSpan: Double,
    public val saturation: Double,
    public val contrast: Double,
) {
    PAST_HILL("landscape_past_hill", 0.0, 0.0, 323.0, 87.0, 113.0, 0.5, 0.86),
    FUTURE_HILL("landscape_future_hill", 0.0, 0.0, 394.0, 143.0, 179.0, 0.5, 0.86),
    DISTANT_SLOPE("landscape_distant_slope", 0.0, 0.0, 724.0, 326.0, 222.0, 0.4, 0.84),
    AUTUMN_TREE("landscape_autumn_tree", 186.0, 186.0, 261.0, 326.0, 222.0, 0.55, 0.94),
    FUTURE_TREE("landscape_future_tree", 132.0, 84.0, 363.0, 326.0, 222.0, 0.55, 0.94),
    AUTUMN_LEAF("landscape_autumn_leaf", 0.0, 0.0, 0.0, 326.0, 222.0, 0.55, 1.0);

    public fun width(atHeight: Double): Double = atHeight * sourceWidth / sourceHeight

    public fun visibleWidth(atHeight: Double): Double = atHeight * sourceVisibleWidth / sourceHeight

    /** How to show a band of [visibleHeight]: draw the image this tall, shifted by `offsetY`. */
    public fun crop(visibleHeight: Double): Crop =
        Crop(
            imageHeight = visibleHeight * sourceHeight / cropSpan,
            offsetY = -visibleHeight * cropTop / cropSpan,
        )

    public data class Crop(val imageHeight: Double, val offsetY: Double)
}
