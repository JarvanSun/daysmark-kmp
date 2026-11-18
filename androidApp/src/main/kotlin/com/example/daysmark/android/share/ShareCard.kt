package com.example.daysmark.android.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.daysmark.android.R
import com.example.daysmark.android.designsystem.DaysmarkColors
import com.example.daysmark.android.designsystem.DaysmarkType
import com.example.daysmark.android.designsystem.Dimensions
import com.example.daysmark.android.designsystem.EventIconImage
import com.example.daysmark.android.designsystem.multiplyOver
import com.example.daysmark.android.text.formatGregorian
import com.example.daysmark.android.text.label
import com.example.daysmark.android.text.phrase
import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.TimeOfDay
import com.example.daysmark.feature.events.share.ShareUiState

private val CardWidth = 360.dp
private val CardMinHeight = 450.dp
private val ArtworkHeight = 180.dp
private val IconSize = 112.dp
private val IconOffset = 8.dp
private val RuleWidth = 32.dp

/**
 * The card that is shared as an image: always in the light palette, so it reads the same wherever
 * it is sent.
 */
@Composable
fun ShareCard(content: ShareUiState.Content, modifier: Modifier = Modifier) {
    Column(
        modifier
            .width(CardWidth)
            .heightIn(min = CardMinHeight)
            .background(DaysmarkColors.Light.canvas)
            .padding(Dimensions.spaceXXLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXLarge),
    ) {
        CardArtwork(content)
        CardSummary(content)
        CardFooter(content)
    }
}

@Composable
private fun CardArtwork(content: ShareUiState.Content) {
    Box(
        Modifier.fillMaxWidth().height(ArtworkHeight).background(DaysmarkColors.Light.canvas),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(R.drawable.share_botanical),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = multiplyOver(DaysmarkColors.Light.canvas),
        )
        EventIconImage(content.icon, IconSize, Modifier.offset(y = IconOffset))
    }
}

@Composable
private fun CardSummary(content: ShareUiState.Content) {
    val colors = DaysmarkColors.Light
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium),
    ) {
        Text(
            content.title,
            style = DaysmarkType.heroTitle,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            phrase(content.dayCount, content.phase),
            style = DaysmarkType.heroPhrase,
            color = if (content.dayCount is DayCount.Past) colors.past else colors.upcoming,
            textAlign = TextAlign.Center,
        )
        Text(
            content.dates(),
            style = DaysmarkType.supporting,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (content.includesCategory) {
            Text(
                content.category.label(),
                style = DaysmarkType.rowSecondary,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun CardFooter(content: ShareUiState.Content) {
    val colors = DaysmarkColors.Light
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium),
    ) {
        Box(Modifier.width(RuleWidth).height(1.dp).background(colors.separator))
        Text(
            stringResource(
                R.string.share_reference,
                format(content.asOfDate, content.asOfTime, "yMMMMdjm"),
            ),
            style = DaysmarkType.rowSecondary,
            color = colors.textSecondary,
        )
    }
}

/** "Oct 25, 2026", or "Dec 28, 2026 – Jan 3, 2027" for a range. */
@Composable
private fun ShareUiState.Content.dates(): String {
    val startText = format(start, time, if (time != null) "yMMMdjm" else "yMMMd")
    val last = end ?: return startText
    return stringResource(R.string.share_range, startText, format(last, null, "yMMMd"))
}

@Composable
private fun format(date: CalendarDate, time: TimeOfDay?, skeleton: String): String =
    formatGregorian(date, time, skeleton)
