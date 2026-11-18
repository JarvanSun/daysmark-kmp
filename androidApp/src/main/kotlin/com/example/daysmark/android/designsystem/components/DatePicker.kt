package com.example.daysmark.android.designsystem.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.daysmark.core.domain.model.CalendarDate
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/** Material's date picker in a dialog, limited to [notBefore]…[notAfter] when given. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerFor(
    date: CalendarDate,
    notBefore: CalendarDate?,
    onDismiss: () -> Unit,
    notAfter: CalendarDate? = null,
    onPick: (CalendarDate) -> Unit,
) {
    val minimum = notBefore?.utcMillis()
    val maximum = notAfter?.utcMillis()
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis = date.utcMillis(),
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) =
                        (minimum == null || utcTimeMillis >= minimum) &&
                            (maximum == null || utcTimeMillis <= maximum)
                },
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onPick(calendarDate(it)) } ?: onDismiss()
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    ) {
        DatePicker(state)
    }
}

// Material's date picker works in UTC milliseconds at the start of the day.
private fun CalendarDate.utcMillis(): Long =
    toLocalDate().atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun calendarDate(utcMillis: Long): CalendarDate =
    CalendarDate.of(Instant.fromEpochMilliseconds(utcMillis).toLocalDateTime(TimeZone.UTC).date)
