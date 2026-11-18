package com.example.daysmark.feature.events.onboarding

import com.example.daysmark.core.domain.calculation.CountedEvent
import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.onboarding.StarterEvent
import com.example.daysmark.feature.events.home.LandscapeCounts
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Where the user is in onboarding; everything else in [OnboardingUiState] is derived from it. */
internal data class OnboardingProgress(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val birthday: CalendarDate,
    /** The user moved the picker; until then its date is only a starting point. */
    val choseBirthday: Boolean = false,
    val keepsBirthday: Boolean = false,
    val selection: Set<StarterEvent> = StarterEvent.entries.toSet(),
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
) {
    fun next(): OnboardingProgress =
        when (step) {
            OnboardingStep.WELCOME -> copy(step = OnboardingStep.BIRTHDAY)
            OnboardingStep.BIRTHDAY ->
                if (choseBirthday) copy(step = OnboardingStep.STARTERS, keepsBirthday = true)
                else this
            OnboardingStep.STARTERS -> this
        }

    fun previous(): OnboardingProgress =
        when {
            isSaving -> this
            step == OnboardingStep.STARTERS -> copy(step = OnboardingStep.BIRTHDAY)
            else -> copy(step = OnboardingStep.WELCOME)
        }

    fun toggle(starter: StarterEvent): OnboardingProgress =
        copy(selection = if (starter in selection) selection - starter else selection + starter)

    /** The starters offered, in order; the birthday ones only when the user kept a birthday. */
    fun offered(): List<StarterEvent> = STARTER_ORDER.filter { keepsBirthday || !it.needsBirthday }

    fun toUiState(now: Instant, zone: TimeZone): OnboardingUiState {
        val today = now.toLocalDateTime(zone).date
        val counts =
            StarterEvent.entries
                .mapNotNull { starter ->
                    starter.event("", today, birthday)?.let {
                        starter to CountedEvent.of(it, now, zone).dayCount
                    }
                }
                .toMap()
        val horizon = horizon().mapNotNull { counts[it] }
        return OnboardingUiState(
            step = step,
            birthday = birthday,
            choseBirthday = choseBirthday,
            headline = headline()?.let { counts.starterCount(it) },
            detail =
                StarterEvent.BIRTH_DATE.takeIf { step == OnboardingStep.BIRTHDAY }
                    ?.let { counts.starterCount(it) },
            landscape = LandscapeCounts.of(horizon),
            choices = choices(counts),
            primaryAction = primaryAction(),
            canContinue = step != OnboardingStep.BIRTHDAY || choseBirthday,
            celebratesBirthday =
                step == OnboardingStep.BIRTHDAY &&
                    choseBirthday &&
                    counts[StarterEvent.BIRTHDAY] == DayCount.Today,
            finishAsksForNotifications =
                step == OnboardingStep.STARTERS &&
                    StarterEvent.BIRTHDAY in offered() &&
                    StarterEvent.BIRTHDAY in selection,
            isSaving = isSaving,
            saveFailed = saveFailed,
        )
    }

    /** The dates the step's landscape draws. */
    private fun horizon(): List<StarterEvent> =
        when (step) {
            OnboardingStep.WELCOME -> listOf(StarterEvent.NEXT_NEW_YEAR)
            OnboardingStep.BIRTHDAY ->
                if (choseBirthday)
                    listOf(
                        StarterEvent.NEXT_NEW_YEAR,
                        StarterEvent.BIRTHDAY,
                        StarterEvent.BIRTH_DATE,
                    )
                else listOf(StarterEvent.NEXT_NEW_YEAR)
            OnboardingStep.STARTERS -> offered().filter { it in selection }
        }

    private fun headline(): StarterEvent? =
        when (step) {
            OnboardingStep.WELCOME -> StarterEvent.NEXT_NEW_YEAR
            OnboardingStep.BIRTHDAY -> StarterEvent.BIRTHDAY
            OnboardingStep.STARTERS -> null
        }

    private fun choices(counts: Map<StarterEvent, DayCount>): List<StarterChoice> =
        if (step != OnboardingStep.STARTERS) emptyList()
        else
            offered().mapNotNull { starter ->
                counts[starter]?.let { StarterChoice(starter, it, starter in selection) }
            }

    private fun primaryAction(): OnboardingPrimaryAction =
        when {
            step != OnboardingStep.STARTERS -> OnboardingPrimaryAction.NEXT
            offered().none { it in selection } -> OnboardingPrimaryAction.START_EMPTY
            else -> OnboardingPrimaryAction.CREATE_SELECTED
        }

    private fun Map<StarterEvent, DayCount>.starterCount(starter: StarterEvent) =
        get(starter)?.let {
            StarterCount(starter, it, isPending = starter.needsBirthday && !choseBirthday)
        }

    private companion object {
        val STARTER_ORDER =
            listOf(
                StarterEvent.BIRTHDAY,
                StarterEvent.BIRTH_DATE,
                StarterEvent.NEXT_NEW_YEAR,
                StarterEvent.NEXT_MONTH,
            )
    }
}
