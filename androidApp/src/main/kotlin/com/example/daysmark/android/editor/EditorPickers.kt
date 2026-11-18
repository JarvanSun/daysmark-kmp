package com.example.daysmark.android.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.daysmark.android.designsystem.components.DatePickerFor
import com.example.daysmark.android.text.formatGregorian
import com.example.daysmark.android.text.formatted
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.TimeOfDay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

/** A labelled row whose value opens the date picker. */
@Composable
fun DateItem(
    title: Int,
    date: CalendarDate,
    notBefore: CalendarDate? = null,
    onChange: (CalendarDate) -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        trailingContent = {
            FilledTonalButton(onClick = { picking = true }) { Text(date.formatted()) }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
    if (picking) {
        DatePickerFor(date, notBefore, onDismiss = { picking = false }) {
            picking = false
            onChange(it)
        }
    }
}

/** A labelled row whose value opens the time picker. */
@Composable
fun TimeItem(title: Int, time: TimeOfDay, onChange: (TimeOfDay) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        trailingContent = { TimeButton(time, onChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeButton(time: TimeOfDay, onChange: (TimeOfDay) -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    FilledTonalButton(onClick = { picking = true }) { Text(time.formatted()) }
    if (picking) {
        val state = rememberTimePickerState(time.hour, time.minute)
        AlertDialog(
            onDismissRequest = { picking = false },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        picking = false
                        onChange(TimeOfDay(state.hour, state.minute))
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

/** A value that opens a menu of [choices]; the current one is shown. */
@Composable
fun <T> ChoiceMenu(
    current: T,
    choices: List<T>,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text(label(current)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(label(choice)) },
                    onClick = {
                        open = false
                        onSelect(choice)
                    },
                )
            }
        }
    }
}

fun CalendarDate.plusDays(days: Int): CalendarDate =
    CalendarDate.of(toLocalDate().plus(DatePeriod(days = days)))

@Composable private fun TimeOfDay.formatted(): String = formatGregorian(ANY_DAY, this, "jm")

/** A day to carry a time through the date formatter; only the time is written. */
private val ANY_DAY = CalendarDate(2000, 1, 1)
