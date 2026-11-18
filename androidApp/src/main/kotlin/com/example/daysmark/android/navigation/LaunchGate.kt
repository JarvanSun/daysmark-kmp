package com.example.daysmark.android.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.daysmark.android.designsystem.Daysmark
import com.example.daysmark.android.designsystem.components.LoadFailed
import com.example.daysmark.android.onboarding.OnboardingScreen
import com.example.daysmark.feature.events.onboarding.LaunchAction
import com.example.daysmark.feature.events.onboarding.LaunchDestination
import com.example.daysmark.shared.AppGraph

/**
 * Onboarding on first run, Home ever after. Until the flag is read it shows only the page colour. A
 * launch [request] waits until Home is showing.
 */
@Composable
fun LaunchGate(graph: AppGraph, request: LaunchRequest?, onRequestHandled: () -> Unit) {
    val launch = viewModel { graph.launchViewModel }
    val destination by launch.uiState.collectAsStateWithLifecycle()
    // Only finishing onboarding animates: onboarding fades out completely, then Home fades in, so
    // the two screens are never drawn over each other. An ordinary launch shows Home at once.
    AnimatedContent(
        destination,
        transitionSpec = {
            if (initialState == LaunchDestination.ONBOARDING) {
                fadeIn(tween(FADE_MS, delayMillis = FADE_MS)) togetherWith fadeOut(tween(FADE_MS))
            } else {
                EnterTransition.None togetherWith ExitTransition.None
            }
        },
        // Between the two screens the page colour shows, not the window's default.
        modifier = Modifier.fillMaxSize().background(Daysmark.colors.canvas),
        label = "launch",
    ) { shown ->
        when (shown) {
            LaunchDestination.HOME -> DaysmarkNavHost(graph, request, onRequestHandled)
            LaunchDestination.ONBOARDING ->
                OnboardingScreen(viewModel { graph.onboardingViewModel })
            LaunchDestination.FAILED ->
                LoadFailed(onRetry = { launch.onAction(LaunchAction.Retry) })
            LaunchDestination.LOADING ->
                Box(Modifier.fillMaxSize().background(Daysmark.colors.canvas))
        }
    }
}

private const val FADE_MS = 180
