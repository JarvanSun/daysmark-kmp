package com.example.daysmark.android.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.daysmark.android.R
import com.example.daysmark.android.designsystem.Daysmark
import com.example.daysmark.android.designsystem.Dimensions
import com.example.daysmark.android.designsystem.EventIconImage
import com.example.daysmark.android.text.label
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventIcon
import com.example.daysmark.core.domain.model.Reminder
import com.example.daysmark.core.domain.model.RepeatRule

private val IconChoiceSize = 52.dp

@Composable
internal fun CategoryChoice(selected: EventCategory, onSelect: (EventCategory) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        Modifier.fillMaxWidth().padding(horizontal = Dimensions.pageMargin)
    ) {
        EventCategory.entries.forEachIndexed { index, category ->
            SegmentedButton(
                selected = category == selected,
                onClick = { onSelect(category) },
                shape = SegmentedButtonDefaults.itemShape(index, EventCategory.entries.size),
            ) {
                Text(category.label())
            }
        }
    }
}

@Composable
internal fun ReminderItem(
    reminder: Reminder,
    choices: List<Int>,
    onChange: (Reminder) -> Unit,
    onRemove: () -> Unit,
) {
    ListItem(
        headlineContent = {
            ChoiceMenu(
                current = reminder.daysBefore,
                choices = choices,
                label = { reminderLabel(it) },
                onSelect = { onChange(reminder.copy(daysBefore = it)) },
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimeButton(reminder.time) { onChange(reminder.copy(time = it)) }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.home_row_delete),
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
internal fun reminderLabel(days: Int): String =
    if (days == 0) stringResource(R.string.reminder_on_the_day)
    else pluralStringResource(R.plurals.reminder_days_before, days, days)

@Composable
internal fun RepeatItem(repeat: RepeatRule, onSelect: (RepeatRule) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.editor_repeat)) },
        trailingContent = {
            ChoiceMenu(repeat, RepeatRule.entries, label = { it.label() }, onSelect = onSelect)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
fun RepeatRule.label(): String =
    stringResource(
        when (this) {
            RepeatRule.NEVER -> R.string.repeat_never
            RepeatRule.MONTHLY -> R.string.repeat_monthly
            RepeatRule.YEARLY -> R.string.repeat_yearly
        }
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IconGrid(selected: EventIcon, onSelect: (EventIcon) -> Unit) {
    FlowRow(
        Modifier.padding(horizontal = Dimensions.pageMargin),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
    ) {
        EventIcon.entries.forEach { icon ->
            Box(
                Modifier.size(IconChoiceSize)
                    .background(
                        if (icon == selected) Daysmark.colors.tintSoft else Color.Transparent,
                        RoundedCornerShape(12.dp),
                    )
                    .selectable(selected = icon == selected, role = Role.RadioButton) {
                        onSelect(icon)
                    },
                contentAlignment = Alignment.Center,
            ) {
                EventIconImage(icon, Dimensions.eventIconSize)
            }
        }
    }
}
