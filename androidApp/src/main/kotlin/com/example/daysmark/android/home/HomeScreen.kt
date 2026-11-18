package com.example.daysmark.android.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daysmark.android.R
import com.example.daysmark.android.designsystem.Daysmark
import com.example.daysmark.android.designsystem.Dimensions
import com.example.daysmark.android.designsystem.components.FailureDialog
import com.example.daysmark.android.designsystem.components.Landscape
import com.example.daysmark.android.designsystem.components.LoadFailed
import com.example.daysmark.android.navigation.Navigator
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.feature.events.home.EventRow
import com.example.daysmark.feature.events.home.HomeAction
import com.example.daysmark.feature.events.home.HomeFailure
import com.example.daysmark.feature.events.home.HomeUiState
import com.example.daysmark.feature.events.home.HomeViewModel
import kotlinx.coroutines.launch

/** Home renders [HomeUiState] and sends [HomeAction]s; it computes nothing itself. */
@Composable
fun HomeScreen(viewModel: HomeViewModel, navigator: Navigator) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDeletion by rememberSaveable { mutableStateOf<String?>(null) }
    val content = state as? HomeUiState.Content
    // A new event starts in the category Home is filtered to.
    val addEvent = {
        navigator.newEvent(
            content?.filters?.firstOrNull { it.isSelected }?.category ?: EventCategory.IMPORTANT
        )
    }

    Scaffold(
        containerColor = Daysmark.colors.canvas,
        topBar = { HomeTopBar(onSettings = navigator::openSettings) },
        floatingActionButton = {
            FloatingActionButton(onClick = addEvent) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.home_toolbar_add),
                )
            }
        },
    ) { padding ->
        if (state == HomeUiState.Failed) {
            LoadFailed(
                onRetry = { viewModel.onAction(HomeAction.Retry) },
                Modifier.padding(padding),
            )
        }
        if (content != null) {
            HomeList(
                content = content,
                onAction = viewModel::onAction,
                onRequestDelete = { pendingDeletion = it.id.value },
                onOpen = { navigator.openDetail(it.id) },
                onAdd = addEvent,
                modifier = Modifier.padding(padding),
            )
        }
    }

    content?.failure?.let { failure ->
        FailureDialog(
            title =
                if (failure == HomeFailure.PINNING) R.string.failure_pin_title
                else R.string.failure_delete_title,
            onDismiss = { viewModel.onAction(HomeAction.DismissFailure) },
        )
    }

    val deleting = content?.rows?.firstOrNull { it.id.value == pendingDeletion }
    if (deleting != null) {
        DeleteDialog(
            row = deleting,
            onConfirm = {
                viewModel.onAction(HomeAction.Delete(deleting.id))
                pendingDeletion = null
            },
            onDismiss = { pendingDeletion = null },
        )
    }
}

@Composable
private fun HomeList(
    content: HomeUiState.Content,
    onAction: (HomeAction) -> Unit,
    onRequestDelete: (EventRow) -> Unit,
    onOpen: (EventRow) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize()) {
        if (content.showsOverview) {
            item(key = "overview") {
                Box(Modifier.padding(horizontal = Dimensions.pageMargin)) {
                    Column {
                        content.focus?.let {
                            FocusCard(it, Modifier.padding(bottom = Dimensions.spaceXLarge))
                        }
                        Landscape(
                            content.landscape,
                            Modifier.padding(bottom = Dimensions.spaceXLarge),
                        )
                        FilterBar(content.filters) { onAction(HomeAction.SelectCategory(it)) }
                    }
                }
            }
        }
        items(content.rows, key = { it.id.value }) { row ->
            SwipeableRow(
                row = row,
                onTogglePin = { onAction(HomeAction.TogglePin(row.id)) },
                onDelete = { onRequestDelete(row) },
                onOpen = { onOpen(row) },
            )
            HorizontalDivider(
                Modifier.padding(
                    start =
                        Dimensions.pageMargin + Dimensions.eventIconSize + Dimensions.spaceMedium
                ),
                color = Daysmark.colors.separator,
            )
        }
        content.placeholder?.let { placeholder ->
            item(key = "placeholder") {
                HomeEmpty(
                    placeholder,
                    onAdd = onAdd,
                    onResetFilters = { onAction(HomeAction.ResetFilters) },
                )
            }
        }
    }
}

/**
 * Swipe right to pin or unpin, left to delete (which asks first). The row snaps back either way: it
 * never disappears by itself.
 */
@Composable
private fun SwipeableRow(
    row: EventRow,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = state,
        backgroundContent = { SwipeBackground(state.dismissDirection, row.isPinned) },
        onDismiss = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> onTogglePin()
                SwipeToDismissBoxValue.EndToStart -> onDelete()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            scope.launch { state.reset() }
        },
    ) {
        EventListItem(row, Modifier.background(Daysmark.colors.canvas).clickable(onClick = onOpen))
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue, isPinned: Boolean) {
    val colors = Daysmark.colors
    val (color, label, alignment) =
        when (direction) {
            SwipeToDismissBoxValue.StartToEnd ->
                Triple(
                    colors.tint,
                    stringResource(
                        if (isPinned) R.string.home_row_unpin else R.string.home_row_pin
                    ),
                    Alignment.CenterStart,
                )
            SwipeToDismissBoxValue.EndToStart ->
                Triple(
                    colors.destructive,
                    stringResource(R.string.home_row_delete),
                    Alignment.CenterEnd,
                )
            SwipeToDismissBoxValue.Settled -> Triple(colors.canvas, "", Alignment.Center)
        }
    Box(
        Modifier.fillMaxSize().background(color).padding(horizontal = Dimensions.pageMargin),
        contentAlignment = alignment,
    ) {
        Text(label, color = colors.textOnTint)
    }
}

@Composable
private fun DeleteDialog(row: EventRow, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_delete_title, row.title)) },
        text = { Text(stringResource(R.string.home_delete_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.home_delete_confirm),
                    color = Daysmark.colors.destructive,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(onSettings: () -> Unit) {
    TopAppBar(
        title = {},
        actions = {
            IconButton(onClick = onSettings) {
                Icon(
                    painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.home_toolbar_settings),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}
