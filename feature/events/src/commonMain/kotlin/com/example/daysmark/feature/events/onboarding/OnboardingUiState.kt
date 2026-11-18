package com.example.daysmark.feature.events.onboarding

import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.onboarding.StarterEvent
import com.example.daysmark.feature.events.home.LandscapeCounts

/** Welcome, then the user's birthday, then the ready-made events to start with. */
public enum class OnboardingStep {
    WELCOME,
    BIRTHDAY,
    STARTERS,
}

/** What the big button at the bottom does on this step. */
public enum class OnboardingPrimaryAction {
    NEXT,
    /** Creates the selected starters. */
    CREATE_SELECTED,
    /** Nothing is selected: starts with an empty list. */
    START_EMPTY,
}

/**
 * A starter with its distance from today: "New Year, in 44 days". [isPending] when it depends on a
 * birthday the user hasn't picked yet: the platform shows the title with a placeholder in place of
 * the distance, so nothing moves when the real one arrives.
 */
public data class StarterCount(
    val starter: StarterEvent,
    val dayCount: DayCount,
    val isPending: Boolean = false,
)

public data class StarterChoice(
    val starter: StarterEvent,
    val dayCount: DayCount,
    val isSelected: Boolean,
)

public data class OnboardingUiState(
    val step: OnboardingStep,
    /** The date the birthday picker shows; used only if the user keeps it. */
    val birthday: CalendarDate,
    /** The user picked [birthday]; before that it is only where the picker starts. */
    val choseBirthday: Boolean,
    /** The large line under the step's title. */
    val headline: StarterCount?,
    /** A smaller second line (the birth date, on the birthday step). */
    val detail: StarterCount?,
    /** The step's dates as the Home landscape draws them. */
    val landscape: LandscapeCounts,
    /** On the last step, the starters offered, in order. */
    val choices: List<StarterChoice>,
    val primaryAction: OnboardingPrimaryAction,
    /** The primary button is enabled: on the birthday step, only once a date is picked. */
    val canContinue: Boolean,
    /** The chosen birthday is today. */
    val celebratesBirthday: Boolean,
    /**
     * Finishing now creates events with reminders (the birthday), so the platform asks for
     * notification permission as it finishes. Onboarding asks for nothing else.
     */
    val finishAsksForNotifications: Boolean,
    /** Saving (a local write, done in milliseconds); further taps are ignored meanwhile. */
    val isSaving: Boolean,
    /** Saving failed; the platform offers to try again. */
    val saveFailed: Boolean,
) {
    /** Back is offered after the first step, and not while saving. */
    val canGoBack: Boolean
        get() = step != OnboardingStep.WELCOME && !isSaving
}
