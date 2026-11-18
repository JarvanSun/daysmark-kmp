package com.example.daysmark.android.detail

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.example.daysmark.android.R
import com.example.daysmark.android.designsystem.Daysmark
import com.example.daysmark.android.designsystem.DaysmarkType
import com.example.daysmark.android.designsystem.Dimensions
import com.example.daysmark.android.text.formatGregorian
import com.example.daysmark.feature.events.detail.CountDirection
import com.example.daysmark.feature.events.detail.DetailCount
import com.example.daysmark.feature.events.detail.DetailUiState
import com.example.daysmark.feature.events.detail.DetailUnit
import com.example.daysmark.feature.events.detail.Elapsed
import java.text.NumberFormat

private val NumberSize = 96.sp
private val CaptionSize = 20.sp
private val TickerSize = 19.sp

fun DetailCount.isPast(): Boolean = direction == CountDirection.PAST

/** The large number and its caption: "547 · days since", or "Today". */
@Composable
fun CountBlock(count: DetailCount, tint: Color) {
    val locale = LocalConfiguration.current.locales[0]
    AnimatedContent(targetState = count, label = "count") { shown ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (shown.direction == CountDirection.TODAY) stringResource(R.string.phrase_today)
                else NumberFormat.getIntegerInstance(locale).format(shown.value),
                fontSize = NumberSize,
                fontWeight = FontWeight.Black,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            Text(
                shown.caption(),
                fontSize = CaptionSize,
                fontWeight = FontWeight.SemiBold,
                color = Daysmark.colors.textSecondary,
            )
            if (shown.remainderDays > 0) {
                Text(
                    pluralStringResource(
                        R.plurals.detail_remainder,
                        shown.remainderDays,
                        shown.remainderDays,
                    ),
                    style = DaysmarkType.supporting,
                    color = Daysmark.colors.textSecondary,
                )
            }
        }
    }
}

/** "547d 9h 41m 0s" in the user's language, as ICU writes durations. */
@Composable
fun Ticker(elapsed: Elapsed) {
    val locale = LocalConfiguration.current.locales[0]
    val text =
        MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.NARROW)
            .formatMeasures(
                Measure(elapsed.days, MeasureUnit.DAY),
                Measure(elapsed.hours, MeasureUnit.HOUR),
                Measure(elapsed.minutes, MeasureUnit.MINUTE),
                Measure(elapsed.seconds, MeasureUnit.SECOND),
            )
    Text(
        text,
        fontSize = TickerSize,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        color = Daysmark.colors.textSecondary,
        modifier = Modifier.padding(top = Dimensions.spaceXLarge),
    )
}

@Composable
private fun DetailCount.caption(): String {
    if (direction == CountDirection.TODAY) return stringResource(R.string.detail_caption_today)
    val past = direction == CountDirection.PAST
    val plural =
        when (unit) {
            DetailUnit.DAYS ->
                if (past) R.plurals.detail_caption_past_days
                else R.plurals.detail_caption_upcoming_days
            DetailUnit.WEEKS ->
                if (past) R.plurals.detail_caption_past_weeks
                else R.plurals.detail_caption_upcoming_weeks
            DetailUnit.HOURS ->
                if (past) R.plurals.detail_caption_past_hours
                else R.plurals.detail_caption_upcoming_hours
            DetailUnit.MINUTES ->
                if (past) R.plurals.detail_caption_past_minutes
                else R.plurals.detail_caption_upcoming_minutes
        }
    return pluralStringResource(plural, value.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
}

/** "Tue, Apr 8, 2025" in the locale's own pattern, with the time when there is one. */
@Composable
fun DetailUiState.Content.formattedDate(): String =
    formatGregorian(date, time, "yMMMdEEE" + (if (time != null) "jm" else ""))
