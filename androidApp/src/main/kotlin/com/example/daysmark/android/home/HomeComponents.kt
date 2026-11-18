package com.example.daysmark.android.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.example.daysmark.android.R
import com.example.daysmark.android.designsystem.Daysmark
import com.example.daysmark.android.designsystem.DaysmarkType
import com.example.daysmark.android.designsystem.Dimensions
import com.example.daysmark.android.designsystem.EventIconImage
import com.example.daysmark.android.designsystem.color
import com.example.daysmark.android.text.formattedDate
import com.example.daysmark.android.text.label
import com.example.daysmark.android.text.phrase
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.feature.events.home.CategoryFilter
import com.example.daysmark.feature.events.home.EventRow

/** The focused event at the top of Home: the pinned one, or the nearest. */
@Composable
fun FocusCard(row: EventRow, modifier: Modifier = Modifier) {
    val phrase = row.phrase()
    Column(
        modifier.fillMaxWidth().padding(top = Dimensions.spaceSmall).clearAndSetSemantics {
            contentDescription = "${row.title}, $phrase"
        },
        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
    ) {
        Text(row.title, style = DaysmarkType.heroTitle, color = Daysmark.colors.textPrimary)
        DayCountText(row, DaysmarkType.heroPhrase)
    }
}

/** A distance phrase in its direction's colour; it slides when the day changes. */
@Composable
fun DayCountText(row: EventRow, style: TextStyle) {
    AnimatedContent(targetState = row.phrase() to row.dayCount, label = "dayCount") {
        (phrase, count) ->
        Text(phrase, style = style, color = count.color())
    }
}

/** One row of category filters with "All" first. The selected one is tinted and bold. */
@Composable
fun FilterBar(filters: List<CategoryFilter>, onSelect: (EventCategory?) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium),
    ) {
        filters.forEach { filter ->
            Box(
                Modifier.selectable(
                        selected = filter.isSelected,
                        role = Role.Tab,
                        onClick = { onSelect(filter.category) },
                    )
                    .defaultMinSize(Dimensions.minimumTapTarget, Dimensions.minimumTapTarget),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = filter.category?.label() ?: stringResource(R.string.home_filter_all),
                    style =
                        if (filter.isSelected) DaysmarkType.filterLabelSelected
                        else DaysmarkType.filterLabel,
                    color =
                        if (filter.isSelected) Daysmark.colors.tint
                        else Daysmark.colors.textSecondary,
                )
            }
        }
    }
}

/** One event in Home's list: icon, title, phrase and date. Long titles wrap to two lines. */
@Composable
fun EventListItem(row: EventRow, modifier: Modifier = Modifier) {
    val phrase = row.phrase()
    val date = row.formattedDate()
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.pageMargin, vertical = Dimensions.spaceMedium)
            .clearAndSetSemantics {
                contentDescription =
                    listOf(row.title, phrase, date).filter { it.isNotEmpty() }.joinToString(", ")
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium),
    ) {
        EventIconImage(row.icon, Dimensions.eventIconSize)
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimensions.rowLineSpacing),
        ) {
            Text(
                row.title,
                style = DaysmarkType.eventTitle,
                color = Daysmark.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            DayCountText(row, DaysmarkType.rowPhrase)
            Text(date, style = DaysmarkType.rowSecondary, color = Daysmark.colors.textSecondary)
        }
    }
}
