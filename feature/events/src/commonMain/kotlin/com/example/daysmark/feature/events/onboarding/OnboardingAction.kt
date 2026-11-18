package com.example.daysmark.feature.events.onboarding

import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.onboarding.StarterEvent

public sealed interface OnboardingAction {
    /** Welcome → birthday → starters. On the birthday step this keeps the birthday. */
    public data object Continue : OnboardingAction

    public data object Back : OnboardingAction

    public data object SkipBirthday : OnboardingAction

    public data class SetBirthday(val date: CalendarDate) : OnboardingAction

    public data class Toggle(val starter: StarterEvent) : OnboardingAction

    /**
     * Creates the selected starters, named with [titles] (the platform's localized names), and
     * finishes. With nothing selected it just finishes.
     */
    public data class Finish(val titles: Map<StarterEvent, String>) : OnboardingAction

    /** Finishes at once with no events (Skip on the welcome step). */
    public data object StartEmpty : OnboardingAction

    /** After a failed save: repeats exactly what the user asked for (starters or none). */
    public data object RetrySave : OnboardingAction

    public data object DismissSaveFailure : OnboardingAction
}
