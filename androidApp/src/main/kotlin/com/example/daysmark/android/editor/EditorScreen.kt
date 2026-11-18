package com.example.daysmark.android.editor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.daysmark.android.R
import com.example.daysmark.android.designsystem.Daysmark
import com.example.daysmark.android.designsystem.Dimensions
import com.example.daysmark.android.designsystem.components.FailureDialog
import com.example.daysmark.android.designsystem.components.LoadFailed
import com.example.daysmark.feature.events.editor.EditorAction
import com.example.daysmark.feature.events.editor.EditorFailure
import com.example.daysmark.feature.events.editor.EditorTarget
import com.example.daysmark.feature.events.editor.EditorUiState
import com.example.daysmark.feature.events.editor.EditorViewModel
import com.example.daysmark.shared.AppGraph
import com.example.daysmark.shared.editor.EditorSessionCodec

/**
 * The editor with its session kept in the screen's saved state: if Android kills the process while
 * the editor is in the background, it comes back with the same draft ([EditorTarget.Resume]).
 */
@Composable
fun EditorEntry(graph: AppGraph, target: EditorTarget, onClose: () -> Unit) {
    var saved by rememberSaveable { mutableStateOf<String?>(null) }
    val viewModel = viewModel {
        val restored = saved?.let(EditorSessionCodec::decode)
        graph.editorViewModelFactory.create(restored?.let(EditorTarget::Resume) ?: target)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        (state as? EditorUiState.Editing)?.let { saved = EditorSessionCodec.encode(it.session) }
    }
    EditorScreen(viewModel, onClose)
}

/** One screen creates and edits an event. It renders [EditorUiState] and sends [EditorAction]s. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: EditorViewModel, onClose: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state) { if (state == EditorUiState.Finished) onClose() }
    if (state == EditorUiState.Failed)
        LoadFailed(onRetry = { viewModel.onAction(EditorAction.Retry) })
    else EditorForm(state as? EditorUiState.Editing, viewModel::onAction, onClose)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorForm(
    editing: EditorUiState.Editing?,
    send: (EditorAction) -> Unit,
    onClose: () -> Unit,
) {
    var confirmingDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    // Unsaved changes are never lost silently: back asks first.
    BackHandler(enabled = editing?.hasChanges == true) { confirmingDiscard = true }
    val requestPermission = rememberNotificationPermission()

    Scaffold(
        containerColor = Daysmark.colors.canvas,
        topBar = {
            EditorTopBar(
                isNew = editing?.isNew != false,
                canSave = editing?.canSave == true,
                onClose = {
                    if (editing?.hasChanges == true) confirmingDiscard = true else onClose()
                },
                onSave = { send(EditorAction.Save) },
            )
        },
    ) { padding ->
        if (editing != null) {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
            ) {
                editorSections(
                    state = editing,
                    send = send,
                    onAddReminder = {
                        send(EditorAction.AddReminder)
                        requestPermission()
                    },
                    onDelete = { confirmingDelete = true },
                )
            }
        }
    }

    if (confirmingDiscard)
        DiscardDialog(onDiscard = onClose, onKeep = { confirmingDiscard = false })
    editing?.failure?.let { failure ->
        FailureDialog(
            title =
                if (failure == EditorFailure.DELETING) R.string.failure_delete_title
                else R.string.failure_save_title,
            onDismiss = { send(EditorAction.DismissFailure) },
        )
    }
    if (confirmingDelete) {
        DeleteDialog(
            onDelete = {
                confirmingDelete = false
                send(EditorAction.Delete)
            },
            onCancel = { confirmingDelete = false },
        )
    }
}

/** Asks for the notification permission when the user first adds a reminder, never at launch. */
@Composable
private fun rememberNotificationPermission(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    isNew: Boolean,
    canSave: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                stringResource(if (isNew) R.string.editor_title_new else R.string.editor_title_edit)
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.editor_cancel),
                )
            }
        },
        actions = {
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.padding(end = Dimensions.spaceSmall),
            ) {
                Text(stringResource(R.string.editor_save))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Daysmark.colors.canvas),
    )
}

@Composable
private fun DiscardDialog(onDiscard: () -> Unit, onKeep: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeep,
        title = { Text(stringResource(R.string.editor_discard_title)) },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.editor_discard), color = Daysmark.colors.destructive)
            }
        },
        dismissButton = {
            TextButton(onClick = onKeep) { Text(stringResource(R.string.editor_keep_editing)) }
        },
    )
}

@Composable
private fun DeleteDialog(onDelete: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.editor_delete_title)) },
        text = { Text(stringResource(R.string.home_delete_message)) },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text(
                    stringResource(R.string.home_delete_confirm),
                    color = Daysmark.colors.destructive,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
