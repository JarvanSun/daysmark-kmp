package com.example.daysmark.android.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daysmark.android.R
import com.example.daysmark.android.designsystem.Daysmark
import com.example.daysmark.android.designsystem.DaysmarkType
import com.example.daysmark.android.designsystem.Dimensions
import com.example.daysmark.android.designsystem.color
import com.example.daysmark.android.text.phrase
import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.model.TimeOfDay
import com.example.daysmark.feature.events.editor.EditorAction
import com.example.daysmark.feature.events.editor.EditorPreview
import com.example.daysmark.feature.events.editor.EditorUiState

private val PreviewNumberSize = 30.sp
private val CardShape = RoundedCornerShape(14.dp)

/** The editor's sections, in the original's order. */
fun LazyListScope.editorSections(
    state: EditorUiState.Editing,
    send: (EditorAction) -> Unit,
    onAddReminder: () -> Unit,
    onDelete: () -> Unit,
) {
    val draft = state.draft
    item(key = "preview") { PreviewCard(draft.title, state.preview) }
    item(key = "title") {
        TitleField(draft.title, focusOnAppear = state.isNew) { send(EditorAction.SetTitle(it)) }
    }
    item(key = "dates") { DatesSection(state, send) }
    item(key = "category") {
        Section(R.string.editor_category) {
            CategoryChoice(draft.category) { send(EditorAction.SetCategory(it)) }
        }
    }
    item(key = "rulesHeader") { SectionHeader(R.string.editor_rules) }
    itemsIndexed(draft.reminders, key = { index, _ -> "reminder-$index" }) { index, reminder ->
        ReminderItem(
            reminder,
            state.reminderChoices,
            onChange = { send(EditorAction.UpdateReminder(index, it)) },
            onRemove = { send(EditorAction.RemoveReminder(index)) },
        )
    }
    item(key = "rules") {
        Column {
            TextButton(
                onClick = onAddReminder,
                modifier = Modifier.padding(horizontal = Dimensions.spaceSmall),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(
                    stringResource(R.string.editor_add_reminder),
                    Modifier.padding(start = Dimensions.spaceSmall),
                )
            }
            RepeatItem(draft.repeat) { send(EditorAction.SetRepeat(it)) }
        }
    }
    item(key = "icons") {
        Section(R.string.editor_appearance) {
            IconGrid(draft.icon) { send(EditorAction.SetIcon(it)) }
        }
    }
    if (!state.isNew) {
        item(key = "delete") {
            TextButton(
                onClick = onDelete,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth().padding(Dimensions.pageMargin),
            ) {
                Text(stringResource(R.string.editor_delete), color = Daysmark.colors.destructive)
            }
        }
    }
}

/** The live preview: "Mia's birthday · in 18 days", as a large number. */
@Composable
private fun PreviewCard(title: String, preview: EditorPreview) {
    val name = title.trim().ifEmpty { stringResource(R.string.editor_untitled) }
    val days =
        when (val count = preview.dayCount) {
            is DayCount.Future -> count.days
            is DayCount.Past -> count.days
            else -> null
        }
    val caption =
        when (preview.dayCount) {
            is DayCount.Future -> stringResource(R.string.editor_preview_upcoming, name)
            is DayCount.Past -> stringResource(R.string.editor_preview_past, name)
            else -> stringResource(R.string.editor_preview_today, name)
        }
    Row(
        Modifier.padding(horizontal = Dimensions.pageMargin)
            .fillMaxWidth()
            .background(Daysmark.colors.canvasSecondary, CardShape)
            .padding(Dimensions.spaceMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            caption,
            style = DaysmarkType.rowSecondary,
            color = Daysmark.colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        if (days != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$days",
                    fontSize = PreviewNumberSize,
                    fontWeight = FontWeight.Black,
                    color = preview.dayCount.color(),
                )
                Text(
                    pluralStringResource(R.plurals.editor_preview_unit, days),
                    style = DaysmarkType.rowSecondary,
                    color = Daysmark.colors.textSecondary,
                )
            }
        } else {
            Text(
                phrase(preview.dayCount, preview.phase),
                style = DaysmarkType.placeholderTitle,
                color = Daysmark.colors.upcoming,
            )
        }
    }
}

@Composable
private fun TitleField(title: String, focusOnAppear: Boolean, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    // A new event needs a title first; an existing one opens without the keyboard.
    LaunchedEffect(Unit) { if (focusOnAppear) focus.requestFocus() }
    TextField(
        value = title,
        onValueChange = onChange,
        placeholder = {
            Text(stringResource(R.string.editor_title_placeholder), style = DaysmarkType.heroTitle)
        },
        textStyle = DaysmarkType.heroTitle.copy(color = Daysmark.colors.textPrimary),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = Dimensions.spaceSmall)
                .focusRequester(focus),
    )
}

@Composable
private fun DatesSection(state: EditorUiState.Editing, send: (EditorAction) -> Unit) {
    val draft = state.draft
    Column {
        DateItem(R.string.editor_start, draft.date) { send(EditorAction.SetDate(it)) }
        SwitchItem(R.string.editor_add_time, draft.time != null) {
            send(EditorAction.SetTime(if (it) TimeOfDay(DEFAULT_EVENT_HOUR, 0) else null))
        }
        draft.time?.let { time ->
            TimeItem(R.string.editor_time, time) { send(EditorAction.SetTime(it)) }
        }
        SwitchItem(R.string.editor_add_end, draft.endDate != null) {
            send(EditorAction.SetEndDate(if (it) draft.date.plusDays(1) else null))
        }
        draft.endDate?.let { end ->
            DateItem(R.string.editor_end, end, notBefore = draft.date) {
                send(EditorAction.SetEndDate(it))
            }
        }
    }
}

private const val DEFAULT_EVENT_HOUR = 19

@Composable
private fun Section(title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
        SectionHeader(title)
        content()
    }
}

@Composable
private fun SectionHeader(title: Int) {
    Column {
        HorizontalDivider(color = Daysmark.colors.separator)
        Text(
            stringResource(title),
            style = DaysmarkType.rowSecondary,
            color = Daysmark.colors.textSecondary,
            modifier =
                Modifier.padding(start = Dimensions.pageMargin, top = Dimensions.spaceMedium),
        )
    }
}

@Composable
private fun SwitchItem(title: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(role = Role.Switch) { onChange(!checked) },
    )
}
