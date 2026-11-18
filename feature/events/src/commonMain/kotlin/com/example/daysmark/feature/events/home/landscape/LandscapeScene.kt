package com.example.daysmark.feature.events.home.landscape

import kotlin.math.max
import kotlin.math.min

/**
 * Where every piece of Home's landscape goes, for a given size and number of past and future
 * events: the past on the left, the future on the right, today as a dot in the middle. Shared so
 * SwiftUI and Compose draw the same picture from the same numbers; they only place the artwork.
 *
 * All values are in the platform's layout unit (points on iOS, dp on Android), origin top-left.
 */
public class LandscapeScene(
    public val width: Double,
    public val height: Double,
    private val pastCount: Int,
    private val futureCount: Int,
) {
    public val center: Double = width / 2
    public val artScale: Double = height / DESIGN_HEIGHT
    public val ground: Double = height - GROUND_INSET * artScale
    public val todayMarkerSize: Double = TODAY_MARKER * artScale

    public val pastHill: Hill? = hill(pastCount, isPast = true)
    public val futureHill: Hill? = hill(futureCount, isPast = false)

    /** The pale slope behind the future hill. */
    public val distantHill: Hill? = futureHill?.let {
        val hillHeight = it.height * DISTANT_HILL_RATIO
        Hill(
            artwork = LandscapeArtwork.DISTANT_SLOPE,
            centerX = center * DISTANT_HILL_POSITION,
            width = center * DISTANT_HILL_WIDTH,
            height = hillHeight,
        )
    }

    /** The distant slope fades so it reads as far away; a little less in dark mode. */
    public fun distantHillOpacity(isDark: Boolean): Double =
        if (isDark) DISTANT_OPACITY_DARK else DISTANT_OPACITY_LIGHT

    public val trees: List<Tree> = if (width > 0) pastTrees() + futureTrees() else emptyList()

    /** Leaves drifting from the autumn tree; empty without past events. */
    public val leaves: List<Leaf> =
        trees.firstOrNull { it.isPast }?.let(::leavesAround) ?: emptyList()

    public data class Hill(
        val artwork: LandscapeArtwork,
        val centerX: Double,
        val width: Double,
        /** The visible height; the artwork is cropped to it with [LandscapeArtwork.crop]. */
        val height: Double,
    )

    public data class Tree(
        val artwork: LandscapeArtwork,
        val centerX: Double,
        /** The y of the tree's foot, on the hill's surface. */
        val baseline: Double,
        val height: Double,
        /** The image's full width at [height]; only [visibleWidth] from its leading edge shows. */
        val imageWidth: Double,
        val visibleWidth: Double,
        val isPast: Boolean,
        val isFlipped: Boolean,
        val opacity: Double,
    )

    public data class Leaf(
        val centerX: Double,
        val centerY: Double,
        val size: Double,
        val rotationDegrees: Double,
        val opacity: Double,
    )

    private fun hill(count: Int, isPast: Boolean): Hill? {
        if (count <= 0) return null
        val hillHeight = hillHeight(count, isPast)
        return Hill(
            artwork = if (isPast) LandscapeArtwork.PAST_HILL else LandscapeArtwork.FUTURE_HILL,
            centerX = if (isPast) center / 2 else width - center / 2,
            width = center,
            height = hillHeight,
        )
    }

    private fun hillHeight(count: Int, isPast: Boolean): Double {
        if (count <= 0) return 0.0
        val growth = MIN_HILL_SCALE + HILL_GROWTH * min(count, HILL_CAP) / HILL_CAP
        val design = if (isPast) PAST_HILL_HEIGHT else FUTURE_HILL_HEIGHT
        return design * artScale * growth
    }

    private fun pastTrees(): List<Tree> =
        if (pastCount > 0)
            listOf(
                tree(x = width * AUTUMN_TREE_X, designHeight = AUTUMN_TREE_HEIGHT, isPast = true)
            )
        else emptyList()

    private fun futureTrees(): List<Tree> {
        if (futureCount <= 0) return emptyList()
        val anchor = width * FUTURE_CLUSTER_X
        val cluster =
            min(width / (CLUSTER_REFERENCE_WIDTH * artScale), CLUSTER_SCALE_LIMIT) * artScale
        return FUTURE_CLUSTER.map {
            tree(
                anchor + it.offsetX * cluster,
                designHeight = it.height,
                isPast = false,
                isFlipped = it.isFlipped,
                opacity = it.opacity,
            )
        }
    }

    private fun tree(
        x: Double,
        designHeight: Double,
        isPast: Boolean,
        isFlipped: Boolean = false,
        opacity: Double = 1.0,
    ): Tree {
        val position = if (isPast) x / center else (x - center) / center
        val surface =
            surfaceFraction(position, isPast) *
                hillHeight(if (isPast) pastCount else futureCount, isPast)
        val treeHeight =
            designHeight * artScale * min(width / (TREE_REFERENCE_WIDTH * artScale), 1.0)
        val artwork = if (isPast) LandscapeArtwork.AUTUMN_TREE else LandscapeArtwork.FUTURE_TREE
        return Tree(
            artwork = artwork,
            centerX = x,
            baseline = ground - surface + artScale,
            height = treeHeight,
            imageWidth = artwork.width(atHeight = treeHeight),
            visibleWidth = artwork.visibleWidth(atHeight = treeHeight),
            isPast = isPast,
            isFlipped = isFlipped,
            opacity = opacity,
        )
    }

    private fun leavesAround(autumn: Tree): List<Leaf> {
        val spread = min(width / (LEAF_REFERENCE_WIDTH * artScale), LEAF_SPREAD_LIMIT)
        return LEAVES.map { leaf ->
            Leaf(
                centerX = autumn.centerX + leaf.offsetX * artScale * spread,
                centerY = autumn.baseline - autumn.height + leaf.offsetY * artScale,
                size = leaf.size * artScale,
                rotationDegrees = leaf.rotation,
                opacity = leaf.opacity,
            )
        }
    }

    /** How high the hill's surface is at [position] (0 at today, 1 at the edge), as a fraction. */
    private fun surfaceFraction(position: Double, isPast: Boolean): Double {
        val samples = if (isPast) PAST_SURFACE else FUTURE_SURFACE
        val scaled = min(max(position, 0.0), 1.0) * (samples.size - 1)
        val lower = min(scaled.toInt(), samples.size - 2)
        return samples[lower] + (samples[lower + 1] - samples[lower]) * (scaled - lower)
    }

    private data class LeafSpec(
        val size: Double,
        val rotation: Double,
        val opacity: Double,
        val offsetX: Double,
        val offsetY: Double,
    )

    /** One tree of the future cluster: offset from the anchor, height and fade, in design units. */
    private data class ClusterTree(
        val offsetX: Double,
        val height: Double,
        val isFlipped: Boolean,
        val opacity: Double,
    )

    private companion object {
        const val DESIGN_HEIGHT = 96.0
        const val GROUND_INSET = 8.0
        const val TODAY_MARKER = 8.0
        const val MIN_HILL_SCALE = 0.6
        const val HILL_GROWTH = 0.4
        const val HILL_CAP = 8
        const val PAST_HILL_HEIGHT = 24.0
        const val FUTURE_HILL_HEIGHT = 34.0
        const val DISTANT_HILL_RATIO = 0.78
        const val DISTANT_HILL_WIDTH = 0.82
        const val DISTANT_HILL_POSITION = 1.49
        const val DISTANT_OPACITY_LIGHT = 0.55
        const val DISTANT_OPACITY_DARK = 0.75
        const val AUTUMN_TREE_X = 0.115
        const val AUTUMN_TREE_HEIGHT = 34.0
        const val FUTURE_CLUSTER_X = 0.85
        const val CLUSTER_REFERENCE_WIDTH = 350.0
        const val CLUSTER_SCALE_LIMIT = 1.35
        const val TREE_REFERENCE_WIDTH = 280.0
        const val LEAF_REFERENCE_WIDTH = 350.0
        const val LEAF_SPREAD_LIMIT = 1.4

        val PAST_SURFACE = listOf(0.0, 0.70, 1.0, 0.72, 0.44, 0.25, 0.15, 0.09, 0.04, 0.02, 0.0)
        val FUTURE_SURFACE = listOf(0.0, 0.04, 0.09, 0.16, 0.25, 0.39, 0.57, 0.82, 1.0, 0.79, 0.50)
        /** Back to front: the flipped tree behind, a small one to the side, the main tree. */
        val FUTURE_CLUSTER =
            listOf(
                ClusterTree(offsetX = 6.0, height = 34.0, isFlipped = true, opacity = 0.7),
                ClusterTree(offsetX = -22.0, height = 27.0, isFlipped = false, opacity = 0.86),
                ClusterTree(offsetX = 0.0, height = 46.0, isFlipped = false, opacity = 1.0),
            )
        val LEAVES =
            listOf(
                LeafSpec(
                    size = 8.0,
                    rotation = 12.0,
                    opacity = 0.8,
                    offsetX = 15.0,
                    offsetY = -2.0,
                ),
                LeafSpec(
                    size = 6.0,
                    rotation = -24.0,
                    opacity = 0.6,
                    offsetX = 28.0,
                    offsetY = 1.0,
                ),
                LeafSpec(size = 4.0, rotation = 32.0, opacity = 0.4, offsetX = 40.0, offsetY = 9.0),
            )
    }
}
