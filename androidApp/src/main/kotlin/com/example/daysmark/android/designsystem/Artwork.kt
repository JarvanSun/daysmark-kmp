package com.example.daysmark.android.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.example.daysmark.android.R
import com.example.daysmark.core.domain.model.EventIcon
import com.example.daysmark.feature.events.home.landscape.LandscapeArtwork

/** The shared artwork files (resources/artwork) as Android drawables. */
@get:DrawableRes
val EventIcon.drawable: Int
    get() =
        when (this) {
            EventIcon.STAR -> R.drawable.event_appearance_star
            EventIcon.GIFT -> R.drawable.event_appearance_gift
            EventIcon.HEART -> R.drawable.event_appearance_heart
            EventIcon.AIRPLANE -> R.drawable.event_appearance_airplane
            EventIcon.GRADUATION_CAP -> R.drawable.event_appearance_graduation
            EventIcon.BIRTHDAY_CAKE -> R.drawable.event_appearance_cake
            EventIcon.BRIEFCASE -> R.drawable.event_appearance_briefcase
            EventIcon.HOUSE -> R.drawable.event_appearance_house
            EventIcon.RUNNING -> R.drawable.event_appearance_running
            EventIcon.BOOK -> R.drawable.event_appearance_book
            EventIcon.CALENDAR -> R.drawable.event_appearance_calendar
        }

/** Evens out the drawings' visual weights; the same factors as iOS. */
@Suppress("MagicNumber") // A table of per-drawing measurements.
private val EventIcon.opticalScale: Float
    get() =
        when (this) {
            EventIcon.STAR -> 1.169f
            EventIcon.GIFT -> 1.052f
            EventIcon.HEART -> 1.089f
            EventIcon.AIRPLANE -> 1.248f
            EventIcon.GRADUATION_CAP -> 0.934f
            EventIcon.BIRTHDAY_CAKE -> 0.954f
            EventIcon.BRIEFCASE -> 0.921f
            EventIcon.HOUSE -> 0.946f
            EventIcon.RUNNING -> 0.894f
            EventIcon.BOOK -> 0.947f
            EventIcon.CALENDAR -> 0.914f
        }

@get:DrawableRes
val LandscapeArtwork.drawable: Int
    get() =
        when (this) {
            LandscapeArtwork.PAST_HILL -> R.drawable.landscape_past_hill
            LandscapeArtwork.FUTURE_HILL -> R.drawable.landscape_future_hill
            LandscapeArtwork.DISTANT_SLOPE -> R.drawable.landscape_distant_slope
            LandscapeArtwork.AUTUMN_TREE -> R.drawable.landscape_autumn_tree
            LandscapeArtwork.FUTURE_TREE -> R.drawable.landscape_future_tree
            LandscapeArtwork.AUTUMN_LEAF -> R.drawable.landscape_autumn_leaf
        }

/** An event's illustrated icon, toned down slightly so it sits quietly next to text. */
private const val ICON_SATURATION_LIGHT = 0.82f
private const val ICON_SATURATION_DARK = 0.72f
private const val ICON_BRIGHTNESS_DARK = 0.08f

@Composable
fun EventIconImage(icon: EventIcon, size: Dp, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    Image(
        painter = painterResource(icon.drawable),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter =
            artworkFilter(
                saturation = if (dark) 0.72f else 0.82f,
                brightness = if (dark) 0.08f else 0f,
            ),
        modifier = modifier.size(size).scale(icon.opticalScale),
    )
}
