package com.example.daysmark.android.share

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daysmark.android.R
import com.example.daysmark.android.designsystem.Daysmark
import com.example.daysmark.android.designsystem.Dimensions
import com.example.daysmark.android.designsystem.components.FailureDialog
import com.example.daysmark.android.designsystem.components.LoadFailed
import com.example.daysmark.feature.events.share.ShareAction
import com.example.daysmark.feature.events.share.ShareUiState
import com.example.daysmark.feature.events.share.ShareViewModel
import kotlinx.coroutines.launch

private val CardCorner = RoundedCornerShape(20.dp)

/** Shares an event as a rendered card through the system share sheet. */
@Composable
fun ShareScreen(viewModel: ShareViewModel, onDone: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state) { if (state == ShareUiState.Missing) onDone() }
    if (state == ShareUiState.Failed) {
        LoadFailed(onRetry = { viewModel.onAction(ShareAction.Retry) })
        return
    }
    val content = state as? ShareUiState.Content ?: return
    val context = LocalContext.current
    val sharing = remember(context) { CardSharing(context) }
    var shareFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    if (shareFailed) FailureDialog(R.string.failure_share_title) { shareFailed = false }
    // The card is drawn into this layer too, so the same pixels can be saved and shared.
    val cardLayer = rememberGraphicsLayer()

    Scaffold(containerColor = Daysmark.colors.canvas, topBar = { ShareTopBar(onDone) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = Dimensions.spaceXLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ShareCard(
                    content,
                    Modifier.clip(CardCorner)
                        .border(1.dp, Daysmark.colors.separator, CardCorner)
                        .drawWithContent {
                            cardLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(cardLayer)
                        },
                )
            }
            ShareControls(
                includesCategory = content.includesCategory,
                onIncludesCategory = { viewModel.onAction(ShareAction.SetIncludesCategory(it)) },
                onShare = {
                    scope.launch {
                        val card = cardLayer.toImageBitmap().asAndroidBitmap()
                        if (!sharing.share(card, content.title)) shareFailed = true
                    }
                },
            )
            Spacer(Modifier.height(Dimensions.spaceMedium))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareTopBar(onDone: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.share_title)) },
        actions = { TextButton(onClick = onDone) { Text(stringResource(R.string.share_done)) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Daysmark.colors.canvas),
    )
}

@Composable
private fun ShareControls(
    includesCategory: Boolean,
    onIncludesCategory: (Boolean) -> Unit,
    onShare: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.share_include_category)) },
        trailingContent = {
            Switch(checked = includesCategory, onCheckedChange = onIncludesCategory)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
    Button(onClick = onShare, modifier = Modifier.fillMaxWidth().height(50.dp)) {
        Icon(Icons.Filled.Share, contentDescription = null)
        Text(stringResource(R.string.share_card), Modifier.padding(start = Dimensions.spaceSmall))
    }
}
