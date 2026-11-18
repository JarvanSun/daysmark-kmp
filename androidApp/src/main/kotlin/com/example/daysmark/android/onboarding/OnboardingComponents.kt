package com.example.daysmark.android.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.daysmark.android.R
import com.example.daysmark.android.designsystem.Daysmark
import com.example.daysmark.android.designsystem.DaysmarkType
import com.example.daysmark.android.designsystem.Dimensions
import com.example.daysmark.android.designsystem.color
import com.example.daysmark.android.designsystem.components.DatePickerFor
import com.example.daysmark.android.text.formatted
import com.example.daysmark.android.text.phrase
import com.example.daysmark.core.domain.calculation.RangePhase
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.onboarding.StarterEvent
import com.example.daysmark.feature.events.onboarding.OnboardingAction
import com.example.daysmark.feature.events.onboarding.OnboardingPrimaryAction
import com.example.daysmark.feature.events.onboarding.OnboardingStep
import com.example.daysmark.feature.events.onboarding.OnboardingUiState
import com.example.daysmark.feature.events.onboarding.StarterChoice
import com.example.daysmark.feature.events.onboarding.StarterCount
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** "New Year" over "in 44 days". */
@Composable
fun StarterSentence(count: StarterCount, titleStyle: TextStyle, phraseStyle: TextStyle) {
    val title = stringResource(count.starter.title)
    val phrase = if (count.isPending) PENDING else phrase(count)
    Column(
        Modifier.clearAndSetSemantics { contentDescription = "$title, $phrase" },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = titleStyle, color = Daysmark.colors.textPrimary)
        Text(
            phrase,
            style = phraseStyle,
            color = if (count.isPending) Daysmark.colors.textTertiary else count.dayCount.color(),
        )
    }
}

/** The chosen date, or an invitation to choose one; opens Material's date picker. */
@Composable
fun BirthdayPicker(state: OnboardingUiState, send: (OnboardingAction) -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    FilledTonalButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
        Text(
            if (state.choseBirthday) state.birthday.formatted()
            else stringResource(R.string.onboarding_birthday_choose),
            style = DaysmarkType.buttonLabel,
        )
    }
    if (picking) {
        DatePickerFor(
            date = state.birthday,
            notBefore = null,
            notAfter = CalendarDate.of(Clock.System.todayIn(TimeZone.currentSystemDefault())),
            onDismiss = { picking = false },
        ) {
            picking = false
            send(OnboardingAction.SetBirthday(it))
        }
    }
}

@Composable
fun StarterChoices(choices: List<StarterChoice>, send: (OnboardingAction) -> Unit) {
    Column {
        choices.forEachIndexed { index, choice ->
            if (index > 0) HorizontalDivider(Modifier.padding(start = MARK_INSET.dp))
            StarterChoiceRow(choice) { send(OnboardingAction.Toggle(choice.starter)) }
        }
    }
}

@Composable
private fun StarterChoiceRow(choice: StarterChoice, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .toggleable(
                value = choice.isSelected,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .padding(vertical = Dimensions.spaceMedium),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painterResource(
                if (choice.isSelected) R.drawable.ic_check_circle else R.drawable.ic_circle
            ),
            contentDescription = null,
            tint = if (choice.isSelected) Daysmark.colors.tint else Daysmark.colors.textTertiary,
            modifier = Modifier.size(MARK_SIZE.dp),
        )
        Column(
            Modifier.alpha(if (choice.isSelected) 1f else UNSELECTED_ALPHA),
            verticalArrangement = Arrangement.spacedBy(Dimensions.rowLineSpacing),
        ) {
            Text(
                stringResource(choice.starter.title),
                style = DaysmarkType.eventTitle,
                color = Daysmark.colors.textPrimary,
            )
            Text(
                phrase(StarterCount(choice.starter, choice.dayCount)),
                style = DaysmarkType.rowPhrase,
                color = choice.dayCount.color(),
            )
            if (choice.starter == StarterEvent.BIRTHDAY) {
                Text(
                    stringResource(R.string.onboarding_starter_birthday_reminders),
                    style = DaysmarkType.rowSecondary,
                    color = Daysmark.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun phrase(count: StarterCount): String =
    LocalResources.current.phrase(count.dayCount, RangePhase.POINT)

/** The localized names, which also become the created events' titles. */
@Composable
fun starterTitles(): Map<StarterEvent, String> =
    StarterEvent.entries.associateWith { stringResource(it.title) }

@get:StringRes
val StarterEvent.title: Int
    get() =
        when (this) {
            StarterEvent.BIRTHDAY -> R.string.onboarding_starter_birthday
            StarterEvent.BIRTH_DATE -> R.string.onboarding_starter_birth_date
            StarterEvent.NEXT_NEW_YEAR -> R.string.onboarding_starter_new_year
            StarterEvent.NEXT_MONTH -> R.string.onboarding_starter_next_month
        }

@get:StringRes
val OnboardingStep.title: Int
    get() =
        when (this) {
            OnboardingStep.WELCOME -> R.string.onboarding_welcome_title
            OnboardingStep.BIRTHDAY -> R.string.onboarding_birthday_title
            OnboardingStep.STARTERS -> R.string.onboarding_starters_title
        }

@get:StringRes
val OnboardingStep.message: Int
    get() =
        when (this) {
            OnboardingStep.WELCOME -> R.string.onboarding_welcome_message
            OnboardingStep.BIRTHDAY -> R.string.onboarding_birthday_message
            OnboardingStep.STARTERS -> R.string.onboarding_starters_message
        }

@get:StringRes
val OnboardingPrimaryAction.title: Int
    get() =
        when (this) {
            OnboardingPrimaryAction.NEXT -> R.string.onboarding_next
            OnboardingPrimaryAction.CREATE_SELECTED -> R.string.onboarding_create_selected
            OnboardingPrimaryAction.START_EMPTY -> R.string.onboarding_start_empty
        }

/** In place of a distance that waits for the user's birthday. */
private const val PENDING = "—"
private const val MARK_SIZE = 24
private const val MARK_INSET = MARK_SIZE + 12
private const val UNSELECTED_ALPHA = 0.55f
