package com.example.daysmark.android.designsystem

import androidx.compose.runtime.Composable
import com.example.daysmark.core.domain.calculation.DayCount

/** Upcoming and past differ in colour and in wording, never in colour alone. */
@Composable
fun DayCount.color() = if (isPastSide()) Daysmark.colors.past else Daysmark.colors.upcoming

fun DayCount.isPastSide(): Boolean =
    this is DayCount.Past ||
        (this is DayCount.Hours && !isFuture) ||
        (this is DayCount.Minutes && !isFuture)
