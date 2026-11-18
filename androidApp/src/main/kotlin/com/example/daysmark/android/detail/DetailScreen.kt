package com.example.daysmark.android.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daysmark.android.R
import com.example.daysmark.android.designsystem.Daysmark
import com.example.daysmark.android.designsystem.DaysmarkType
import com.example.daysmark.android.designsystem.Dimensions
import com.example.daysmark.android.designsystem.components.ConfettiOverlay
import com.example.daysmark.android.designsystem.components.FailureDialog
import com.example.daysmark.android.designsystem.components.LoadFailed
import com.example.daysmark.android.text.formattedDate
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.feature.events.detail.DetailAction
import com.example.daysmark.feature.events.detail.DetailUiState
import com.example.daysmark.feature.events.detail.DetailViewModel

private const val GRADIENT_ALPHA = 0.14f

/** The tint fades out by the middle of the screen. */
private const val GRADIENT_END = 0.5f
private val ShareButtonHeight = 50.dp

/**
 * One event: the large count the user can cycle through units, the ticking line, pin, edit, share.
 */
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onEdit: (EventId) -> Unit,
    onShare: (EventId) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state) { if (state == DetailUiState.Missing) onBack() }
    when (val shown = state) {
        DetailUiState.Failed -> LoadFailed(onRetry = { viewModel.onAction(DetailAction.Retry) })
        is DetailUiState.Content -> {
            DetailContent(shown, viewModel::onAction, onBack, onEdit, onShare)
            if (shown.pinFailed) {
                FailureDialog(R.string.failure_pin_title) {
                    viewModel.onAction(DetailAction.DismissFailure)
                }
            }
        }
        DetailUiState.Loading,
        DetailUiState.Missing -> Unit
    }
}

@Composable
private fun DetailContent(
    content: DetailUiState.Content,
    send: (DetailAction) -> Unit,
    onBack: () -> Unit,
    onEdit: (EventId) -> Unit,
    onShare: (EventId) -> Unit,
) {
    val tint = if (content.count.isPast()) Daysmark.colors.past else Daysmark.colors.upcoming
    val haptics = LocalHapticFeedback.current

    Scaffold(
        containerColor = Daysmark.colors.canvas,
        topBar = {
            DetailTopBar(
                isPinned = content.isPinned,
                onBack = onBack,
                onTogglePin = { send(DetailAction.TogglePin) },
                onEdit = { onEdit(content.id) },
            )
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to tint.copy(alpha = GRADIENT_ALPHA),
                        GRADIENT_END to Color.Transparent,
                    )
                )
        ) {
            Column(
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Dimensions.spaceXLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Header(content)
                Spacer(Modifier.weight(1f))
                CountArea(content, tint) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    send(DetailAction.CycleUnit)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { onShare(content.id) },
                    modifier = Modifier.fillMaxWidth().height(ShareButtonHeight),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text(
                        stringResource(R.string.detail_share),
                        Modifier.padding(start = Dimensions.spaceSmall),
                    )
                }
                Spacer(Modifier.height(Dimensions.spaceMedium))
            }
            if (content.isTheDay) ConfettiOverlay()
        }
    }
}

@Composable
private fun Header(content: DetailUiState.Content) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = Dimensions.spaceMedium),
    ) {
        Text(
            content.title,
            style = DaysmarkType.heroPhrase,
            color = Daysmark.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            content.formattedDate(),
            style = DaysmarkType.supporting,
            color = Daysmark.colors.textSecondary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    isPinned: Boolean,
    onBack: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.detail_back),
                )
            }
        },
        actions = {
            IconButton(onClick = onTogglePin) {
                Icon(
                    painterResource(if (isPinned) R.drawable.ic_pin_filled else R.drawable.ic_pin),
                    contentDescription =
                        stringResource(
                            if (isPinned) R.string.home_row_unpin else R.string.home_row_pin
                        ),
                )
            }
            TextButton(onClick = onEdit) { Text(stringResource(R.string.detail_edit)) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}

/** The number (tap to change unit), the ticking line and the hint. */
@Composable
private fun CountArea(content: DetailUiState.Content, tint: Color, onCycleUnit: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier.clickable(
                enabled = content.canSwitchUnit,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCycleUnit,
            ),
    ) {
        CountBlock(content.count, tint)
    }
    content.ticker?.let { Ticker(it) }
    if (content.canSwitchUnit) {
        Text(
            stringResource(R.string.detail_switch_hint),
            style = DaysmarkType.rowSecondary,
            color = Daysmark.colors.textTertiary,
            modifier = Modifier.padding(top = Dimensions.spaceMedium),
        )
    }
}
