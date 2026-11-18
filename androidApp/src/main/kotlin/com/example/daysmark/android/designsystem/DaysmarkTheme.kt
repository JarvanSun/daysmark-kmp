package com.example.daysmark.android.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LocalColors = staticCompositionLocalOf { DaysmarkColors.Light }

/** Daysmark's colours (generated from resources/design-tokens.json, shared with iOS). */
object Daysmark {
    val colors: DaysmarkColors
        @Composable get() = LocalColors.current
}

/** Text styles, matching the iOS roles and scaled to Android's type sizes. */
object DaysmarkType {
    val pageTitle = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
    val pageSubtitle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
    val heroTitle =
        TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
    val heroPhrase =
        TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
    val eventTitle = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
    val rowPhrase = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium)
    val rowSecondary = TextStyle(fontSize = 14.sp, lineHeight = 19.sp)
    val filterLabel = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
    val filterLabelSelected = filterLabel.copy(fontWeight = FontWeight.SemiBold)
    val placeholderTitle =
        TextStyle(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold)
    val noticeTitle = eventTitle
    val supporting = rowSecondary
    val buttonLabel =
        TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
}

/** Spacing and sizes; the same values as iOS, in dp. */
object Dimensions {
    val pageMargin = 20.dp
    val spaceSmall = 8.dp
    val spaceMedium = 12.dp
    val spaceXLarge = 24.dp
    val spaceXXLarge = 32.dp
    val minimumTapTarget = 48.dp
    val rowLineSpacing = 2.dp
    val eventIconSize = 36.dp
    val landscapeHeight = 96.dp
    val placeholderTopInset = 48.dp
    val placeholderMessageSpacing = 10.dp
    val emptyArtworkMaximumWidth = 240.dp
    val filteredArtworkWidth = 164.dp
    val filteredArtworkHeight = 110.dp
}

@Composable
fun DaysmarkTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DaysmarkColors.Dark else DaysmarkColors.Light
    val scheme =
        (if (darkTheme) darkColorScheme() else lightColorScheme()).copy(
            primary = colors.tint,
            onPrimary = colors.textOnTint,
            primaryContainer = colors.tintSoft,
            onPrimaryContainer = colors.tint,
            // Material's selected segments, tonal buttons and chips use the secondary roles.
            secondary = colors.tint,
            onSecondary = colors.textOnTint,
            secondaryContainer = colors.tintSoft,
            onSecondaryContainer = colors.tint,
            surfaceVariant = colors.canvasSecondary,
            surfaceContainerHighest = colors.canvasSecondary,
            surfaceContainerLow = colors.canvas,
            outline = colors.textTertiary,
            background = colors.canvas,
            onBackground = colors.textPrimary,
            surface = colors.canvas,
            onSurface = colors.textPrimary,
            surfaceContainer = colors.canvasSecondary,
            surfaceContainerHigh = colors.canvasSecondary,
            onSurfaceVariant = colors.textSecondary,
            outlineVariant = colors.separator,
            error = colors.destructive,
        )
    CompositionLocalProvider(LocalColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
