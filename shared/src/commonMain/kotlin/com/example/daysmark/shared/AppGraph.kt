package com.example.daysmark.shared

import com.example.daysmark.feature.events.detail.DetailViewModel
import com.example.daysmark.feature.events.editor.EditorViewModel
import com.example.daysmark.feature.events.home.HomeViewModel
import com.example.daysmark.feature.events.onboarding.LaunchViewModel
import com.example.daysmark.feature.events.onboarding.OnboardingViewModel
import com.example.daysmark.feature.events.share.ShareViewModel

/**
 * What the platforms get from the dependency graph. A plain interface, so Swift sees only these
 * members and none of the graph's construction (docs/spec/architecture.md).
 */
public interface AppGraph {
    /** Process-wide work (keeping reminders scheduled); the platform starts it at launch. */
    public val services: AppServices

    /** What the app opens to: onboarding on first run, then Home. */
    public val launchViewModel: LaunchViewModel

    public val onboardingViewModel: OnboardingViewModel

    /** A new ViewModel per call; the platform's ViewModel owner keeps it alive. */
    public val homeViewModel: HomeViewModel

    /** Creates the detail screen's ViewModel for one event. */
    public val detailViewModelFactory: DetailViewModel.Factory

    /** Creates the share screen's ViewModel for one event. */
    public val shareViewModelFactory: ShareViewModel.Factory

    /** Creates the editor for one event, or for a new one. */
    public val editorViewModelFactory: EditorViewModel.Factory
}
