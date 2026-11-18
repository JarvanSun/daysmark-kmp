package com.example.daysmark.android.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daysmark.android.R
import com.example.daysmark.android.designsystem.Daysmark
import com.example.daysmark.android.designsystem.DaysmarkType
import com.example.daysmark.android.designsystem.Dimensions
import com.example.daysmark.android.designsystem.components.ConfettiOverlay
import com.example.daysmark.android.designsystem.components.Landscape
import com.example.daysmark.core.domain.onboarding.StarterEvent
import com.example.daysmark.feature.events.onboarding.OnboardingAction
import com.example.daysmark.feature.events.onboarding.OnboardingPrimaryAction
import com.example.daysmark.feature.events.onboarding.OnboardingStep
import com.example.daysmark.feature.events.onboarding.OnboardingUiState
import com.example.daysmark.feature.events.onboarding.OnboardingViewModel

/**
 * The first-run introduction: welcome, the user's birthday, then ready-made events. Renders
 * [OnboardingUiState] and sends [OnboardingAction]s; the flow's rules live in the shared code.
 */
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val titles = starterTitles()
    val send = viewModel::onAction
    // The birthday comes with reminders; finishing with it asks for notifications (Android 13+).
    val askNotifications =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val finish = { action: OnboardingAction ->
        if (
            action is OnboardingAction.Finish &&
                state.finishAsksForNotifications &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        send(action)
    }
    BackHandler(enabled = state.canGoBack) { send(OnboardingAction.Back) }
    Scaffold(
        containerColor = Daysmark.colors.canvas,
        topBar = { OnboardingTopBar(state, send) },
        bottomBar = { PrimaryButton(state) { finish(state.primaryIntent(titles)) } },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = READABLE_WIDTH.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimensions.pageMargin)
                    .padding(bottom = Dimensions.spaceMedium),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXLarge),
            ) {
                Header(state.step)
                Sentences(state)
                Landscape(state.landscape)
                when (state.step) {
                    OnboardingStep.BIRTHDAY -> BirthdayPicker(state, send)
                    OnboardingStep.STARTERS -> StarterChoices(state.choices, send)
                    OnboardingStep.WELCOME -> Unit
                }
            }
            if (state.celebratesBirthday) ConfettiOverlay()
        }
    }
    if (state.saveFailed) SaveFailedDialog(onRetry = { send(OnboardingAction.RetrySave) }, send)
}

private fun OnboardingUiState.primaryIntent(titles: Map<StarterEvent, String>): OnboardingAction =
    if (primaryAction == OnboardingPrimaryAction.NEXT) OnboardingAction.Continue
    else OnboardingAction.Finish(titles)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingTopBar(state: OnboardingUiState, send: (OnboardingAction) -> Unit) {
    TopAppBar(
        title = {},
        navigationIcon = {
            if (state.canGoBack) {
                IconButton(onClick = { send(OnboardingAction.Back) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.onboarding_back),
                    )
                }
            }
        },
        actions = {
            when (state.step) {
                OnboardingStep.WELCOME ->
                    TextButton(onClick = { send(OnboardingAction.StartEmpty) }) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                OnboardingStep.BIRTHDAY ->
                    TextButton(onClick = { send(OnboardingAction.SkipBirthday) }) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                OnboardingStep.STARTERS -> Unit
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}

@Composable
private fun Header(step: OnboardingStep) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)) {
        Text(
            stringResource(step.title),
            style = DaysmarkType.pageTitle,
            color = Daysmark.colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(step.message),
            style = DaysmarkType.pageSubtitle,
            color = Daysmark.colors.textSecondary,
        )
    }
}

@Composable
private fun Sentences(state: OnboardingUiState) {
    if (state.headline == null && state.detail == null) return
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)) {
        state.headline?.let { StarterSentence(it, DaysmarkType.heroTitle, DaysmarkType.heroPhrase) }
        state.detail?.let { StarterSentence(it, DaysmarkType.eventTitle, DaysmarkType.rowPhrase) }
    }
}

@Composable
private fun PrimaryButton(state: OnboardingUiState, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .background(Daysmark.colors.canvas)
            .navigationBarsPadding()
            .padding(horizontal = Dimensions.pageMargin, vertical = Dimensions.spaceMedium),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = onClick,
            enabled = state.canContinue,
            modifier =
                Modifier.widthIn(max = READABLE_WIDTH.dp).fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text(stringResource(state.primaryAction.title), style = DaysmarkType.buttonLabel)
        }
    }
}

@Composable
private fun SaveFailedDialog(onRetry: () -> Unit, send: (OnboardingAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { send(OnboardingAction.DismissSaveFailure) },
        title = { Text(stringResource(R.string.onboarding_failure_title)) },
        text = { Text(stringResource(R.string.onboarding_failure_message)) },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.onboarding_failure_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = { send(OnboardingAction.DismissSaveFailure) }) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

private const val READABLE_WIDTH = 560
