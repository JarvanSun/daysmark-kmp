package com.example.daysmark.android.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.example.daysmark.android.R
import com.example.daysmark.android.designsystem.Daysmark
import com.example.daysmark.android.designsystem.DaysmarkType
import com.example.daysmark.android.designsystem.Dimensions
import com.example.daysmark.android.designsystem.artworkFilter
import com.example.daysmark.android.designsystem.multiplyOver
import com.example.daysmark.feature.events.home.HomePlaceholder

/** What Home shows instead of rows: the first-date invitation, or a reset for an empty filter. */
@Composable
fun HomeEmpty(placeholder: HomePlaceholder, onAdd: () -> Unit, onResetFilters: () -> Unit) {
    when (placeholder) {
        HomePlaceholder.NO_EVENTS -> NoEvents(onAdd)
        HomePlaceholder.NO_MATCHES -> NoMatches(onResetFilters)
    }
}

@Composable
private fun NoEvents(onAdd: () -> Unit) {
    val dark = isSystemInDarkTheme()
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = Dimensions.pageMargin)
            .padding(top = Dimensions.placeholderTopInset, bottom = Dimensions.spaceXXLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXLarge),
    ) {
        Image(
            painterResource(
                if (dark) R.drawable.home_first_date_dark else R.drawable.home_first_date
            ),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            // The original blends this drawing with the page; on a flat page that is a filter.
            colorFilter =
                if (dark) artworkFilter(screenOver = Daysmark.colors.canvas)
                else multiplyOver(Daysmark.colors.canvas),
            modifier = Modifier.widthIn(max = Dimensions.emptyArtworkMaximumWidth),
        )
        Message(
            R.string.home_empty_title,
            R.string.home_empty_description,
            DaysmarkType.placeholderTitle,
        )
        Button(onClick = onAdd) {
            Text(stringResource(R.string.home_empty_create), style = DaysmarkType.buttonLabel)
        }
    }
}

private const val LEAVES_SATURATION = 0.8f

@Composable
private fun NoMatches(onResetFilters: () -> Unit) {
    val dark = isSystemInDarkTheme()
    Column(
        Modifier.fillMaxWidth()
            .padding(vertical = Dimensions.spaceXXLarge, horizontal = Dimensions.pageMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium),
    ) {
        Image(
            painterResource(R.drawable.home_empty_paper_leaves),
            contentDescription = null,
            colorFilter =
                if (dark) {
                    artworkFilter(
                        saturation = 0f,
                        contrast = -1f,
                        multiply = Daysmark.colors.tint,
                        screenOver = Daysmark.colors.canvas,
                    )
                } else {
                    multiplyOver(Daysmark.colors.canvas, saturation = LEAVES_SATURATION)
                },
            modifier =
                Modifier.size(Dimensions.filteredArtworkWidth, Dimensions.filteredArtworkHeight),
        )
        Message(
            R.string.home_filter_empty_title,
            R.string.home_filter_empty_description,
            DaysmarkType.noticeTitle,
        )
        TextButton(onClick = onResetFilters) { Text(stringResource(R.string.home_filter_reset)) }
    }
}

@Composable
private fun Message(title: Int, detail: Int, titleStyle: TextStyle) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.placeholderMessageSpacing),
    ) {
        Text(
            stringResource(title),
            style = titleStyle,
            color = Daysmark.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(detail),
            style = DaysmarkType.supporting,
            color = Daysmark.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
