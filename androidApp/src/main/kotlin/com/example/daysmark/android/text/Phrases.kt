package com.example.daysmark.android.text

import android.content.res.Resources
import android.icu.text.RelativeDateTimeFormatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.example.daysmark.android.R
import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.calculation.RangePhase
import com.example.daysmark.feature.events.home.EventRow
import java.text.NumberFormat
import java.util.Locale

/**
 * The one localized phrase for a distance (docs/spec/product.md, "Relative time"), for code outside
 * Compose (notifications). The composables use the same function.
 */
fun Resources.phrase(dayCount: DayCount, phase: RangePhase): String {
    val locale = configuration.locales[0]
    val base =
        when (dayCount) {
            DayCount.Today -> getString(R.string.phrase_today)
            DayCount.Now -> getString(R.string.phrase_now)
            is DayCount.Future ->
                if (dayCount.days == 1) getString(R.string.phrase_tomorrow)
                else
                    getQuantityString(
                        R.plurals.phrase_future_days,
                        dayCount.days,
                        dayCount.days.grouped(locale),
                    )
            is DayCount.Past ->
                if (dayCount.days == 1) getString(R.string.phrase_yesterday)
                else
                    getQuantityString(
                        R.plurals.phrase_past_days,
                        dayCount.days,
                        dayCount.days.grouped(locale),
                    )
            is DayCount.Hours ->
                relative(
                    dayCount.hours,
                    dayCount.isFuture,
                    RelativeDateTimeFormatter.RelativeUnit.HOURS,
                    locale,
                )
            is DayCount.Minutes ->
                relative(
                    dayCount.minutes,
                    dayCount.isFuture,
                    RelativeDateTimeFormatter.RelativeUnit.MINUTES,
                    locale,
                )
        }
    val embedded = base.replaceFirstChar { it.lowercase(locale) }
    return when (phase) {
        RangePhase.POINT -> base
        RangePhase.BEFORE_START -> getString(R.string.phrase_range_starts, embedded)
        RangePhase.ONGOING -> getString(R.string.phrase_range_ends, embedded)
        RangePhase.ENDED -> getString(R.string.phrase_range_ended, embedded)
    }
}

/** "9,452" in English, "9452" where the locale doesn't group: what iOS shows too. */
private fun Int.grouped(locale: Locale): String =
    NumberFormat.getIntegerInstance(locale).format(this)

/** "in 3 hours": ICU's own wording for each language. */
private fun relative(
    amount: Int,
    isFuture: Boolean,
    unit: RelativeDateTimeFormatter.RelativeUnit,
    locale: Locale,
) =
    RelativeDateTimeFormatter.getInstance(locale)
        .format(
            amount.toDouble(),
            if (isFuture) RelativeDateTimeFormatter.Direction.NEXT
            else RelativeDateTimeFormatter.Direction.LAST,
            unit,
        )

/** The one localized phrase for a distance; see [Resources.phrase]. */
@Composable fun DayCount.phrase(): String = LocalResources.current.phrase(this, RangePhase.POINT)

/** A distance phrase placed in its range phase: "Starts in 3 days", "Ends tomorrow". */
@Composable
fun phrase(dayCount: DayCount, phase: RangePhase): String =
    LocalResources.current.phrase(dayCount, phase)

/** The phrase every surface shows for this row, placed in its range phase. */
@Composable fun EventRow.phrase(): String = LocalResources.current.phrase(dayCount, phase)
