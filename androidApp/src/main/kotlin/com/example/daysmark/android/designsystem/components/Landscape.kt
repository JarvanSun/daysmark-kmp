package com.example.daysmark.android.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.daysmark.android.R
import com.example.daysmark.android.designsystem.Daysmark
import com.example.daysmark.android.designsystem.Dimensions
import com.example.daysmark.android.designsystem.artworkFilter
import com.example.daysmark.android.designsystem.drawable
import com.example.daysmark.feature.events.home.LandscapeCounts
import com.example.daysmark.feature.events.home.landscape.LandscapeArtwork
import com.example.daysmark.feature.events.home.landscape.LandscapeScene

/**
 * Home's landscape: the past on the left, the future on the right, today as a dot. Every position
 * and toning value comes from the shared [LandscapeScene] and [LandscapeArtwork], so iOS draws the
 * same picture.
 */
@Composable
fun Landscape(counts: LandscapeCounts, modifier: Modifier = Modifier) {
    val description =
        pluralStringResource(R.plurals.landscape_past, counts.past, counts.past) +
            ", " +
            pluralStringResource(R.plurals.landscape_future, counts.future, counts.future)
    BoxWithConstraints(
        modifier.fillMaxWidth().height(Dimensions.landscapeHeight).clipToBounds().semantics {
            contentDescription = description
        }
    ) {
        val scene =
            remember(maxWidth, counts) {
                LandscapeScene(
                    width = maxWidth.value.toDouble(),
                    height = Dimensions.landscapeHeight.value.toDouble(),
                    pastCount = counts.past,
                    futureCount = counts.future,
                )
            }
        val colors = Daysmark.colors
        scene.distantHill?.let {
            Hill(
                it,
                scene.ground,
                colors.landscapeFutureGround,
                Modifier.alpha(scene.distantHillOpacity(isSystemInDarkTheme()).toFloat()),
            )
        }
        scene.trees.forEach { Tree(it) }
        scene.pastHill?.let { Hill(it, scene.ground, colors.landscapePastGround) }
        scene.futureHill?.let {
            Hill(it, scene.ground, colors.landscapeFutureGround, shape = FutureHillEnding)
        }
        scene.leaves.forEach { Leaf(it) }
        GroundAndToday(scene)
    }
}

@Composable
private fun Tree(tree: LandscapeScene.Tree) {
    val colors = Daysmark.colors
    Box(
        Modifier.place(
                tree.centerX,
                tree.baseline - tree.height / 2,
                tree.visibleWidth,
                tree.height,
            )
            .clipToBounds()
            .graphicsLayer {
                scaleX = if (tree.isFlipped) -1f else 1f
                alpha = tree.opacity.toFloat()
            }
    ) {
        Image(
            painterResource(tree.artwork.drawable),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            colorFilter =
                tree.artwork.filter(
                    if (tree.isPast) colors.landscapePastFoliage else colors.landscapeFutureFoliage
                ),
            // The image is wider than what shows; only its leading part is visible.
            modifier =
                Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                    .size(tree.imageWidth.dp, tree.height.dp),
        )
    }
}

/** A hill or slope: only the band of the image that [LandscapeArtwork.crop] names shows. */
@Composable
private fun Hill(
    hill: LandscapeScene.Hill,
    ground: Double,
    tint: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
) {
    val crop = hill.artwork.crop(visibleHeight = hill.height)
    // Position first: a modifier before `offset` (like the caller's alpha layer) would be drawn at
    // the un-offset position and clip the hill away.
    Box(
        Modifier.place(hill.centerX, ground - hill.height / 2, hill.width, hill.height)
            .then(modifier)
            .clip(shape)
    ) {
        Image(
            painterResource(hill.artwork.drawable),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            colorFilter = hill.artwork.filter(tint),
            modifier =
                Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                    .offset(y = crop.offsetY.dp)
                    .size(hill.width.dp, crop.imageHeight.dp),
        )
    }
}

@Composable
private fun Leaf(leaf: LandscapeScene.Leaf) {
    Image(
        painterResource(LandscapeArtwork.AUTUMN_LEAF.drawable),
        contentDescription = null,
        colorFilter = LandscapeArtwork.AUTUMN_LEAF.filter(Daysmark.colors.landscapePastFoliage),
        modifier =
            Modifier.place(leaf.centerX, leaf.centerY, leaf.size, leaf.size)
                .rotate(leaf.rotationDegrees.toFloat())
                .alpha(leaf.opacity.toFloat()),
    )
}

/** The ground line and the dot for today. */
@Composable
private fun GroundAndToday(scene: LandscapeScene) {
    val separator = Daysmark.colors.separator
    val tint = Daysmark.colors.tint
    Canvas(Modifier.size(scene.width.dp, scene.height.dp)) {
        val y = scene.ground.dp.toPx()
        drawLine(separator, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        drawCircle(
            tint,
            radius = (scene.todayMarkerSize / 2).dp.toPx(),
            center = Offset(scene.center.dp.toPx(), y),
        )
    }
}

private fun LandscapeArtwork.filter(tint: Color) =
    artworkFilter(saturation.toFloat(), contrast.toFloat(), multiply = tint)

/** Positions content by its centre, in the scene's dp coordinates. */
private fun Modifier.place(
    centerX: Double,
    centerY: Double,
    width: Double,
    height: Double,
): Modifier = offset((centerX - width / 2).dp, (centerY - height / 2).dp).size(width.dp, height.dp)

/** The future hill's rounded right end; the same curve as iOS. */
@Suppress("MagicNumber") // Bézier control points of the drawn curve, mirrored in iOS.
private val FutureHillEnding = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width * 0.84f, 0f)
    cubicTo(
        size.width * 0.91f,
        size.height * 0.1f,
        size.width * 0.94f,
        size.height * 0.98f,
        size.width,
        size.height,
    )
    lineTo(0f, size.height)
    close()
}
