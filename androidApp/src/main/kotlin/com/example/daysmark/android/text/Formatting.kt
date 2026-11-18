package com.example.daysmark.android.text

import android.icu.text.DateFormat
import android.icu.util.GregorianCalendar
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.example.daysmark.android.R
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.TimeOfDay
import com.example.daysmark.feature.events.home.EventRow
import java.util.Locale

/**
 * Writes a date (and time) as the user's locale does, always in the Gregorian calendar the app
 * counts in. `Calendar.getInstance(locale)` would pick the locale's own calendar (Buddhist for
 * th-TH), and the same date would show a different year. [skeleton] is an ICU skeleton ("yMMMd").
 */
fun formatGregorian(
    date: CalendarDate,
    time: TimeOfDay?,
    skeleton: String,
    locale: Locale,
): String {
    val calendar =
        GregorianCalendar(locale).apply {
            clear()
            set(date.year, date.month - 1, date.day, time?.hour ?: 0, time?.minute ?: 0)
        }
    return DateFormat.getInstanceForSkeleton(GregorianCalendar(locale), skeleton, locale)
        .format(calendar.time)
}

/** [formatGregorian] in the current configuration's locale. */
@Composable
fun formatGregorian(date: CalendarDate, time: TimeOfDay?, skeleton: String): String =
    formatGregorian(date, time, skeleton, LocalConfiguration.current.locales[0])

/** The day as the user's locale writes it: "Oct 25", "Oct 25, 2027", "Oct 25, 7:30 PM". */
@Composable
fun EventRow.formattedDate(): String =
    formatGregorian(
        date,
        time,
        (if (showsYear) "yMMMd" else "MMMd") + (if (time != null) "jm" else ""),
    )

/** The day with its year: "Nov 18, 2026". */
@Composable fun CalendarDate.formatted(): String = formatGregorian(this, null, "yMMMd")

/** A category's name in the user's language. */
@Composable
fun EventCategory.label(): String =
    stringResource(
        when (this) {
            EventCategory.IMPORTANT -> R.string.category_important
            EventCategory.BIRTHDAY -> R.string.category_birthday
            EventCategory.ANNIVERSARY -> R.string.category_anniversary
        }
    )
