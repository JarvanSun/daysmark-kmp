package com.example.daysmark.core.domain.onboarding

import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventIcon
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.Reminder
import com.example.daysmark.core.domain.model.RepeatRule
import com.example.daysmark.core.domain.model.TimeOfDay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * The ready-made events onboarding offers. Each is a real event the user can edit or delete later;
 * its id is fixed, so finishing onboarding twice can't create duplicates.
 */
public enum class StarterEvent(internal val id: String) {
    BIRTHDAY("starter-birthday"),
    BIRTH_DATE("starter-birth-date"),
    NEXT_NEW_YEAR("starter-new-year"),
    NEXT_MONTH("starter-next-month");

    /** Needs the user's birth date; offered only when they gave one. */
    public val needsBirthday: Boolean
        get() = this == BIRTHDAY || this == BIRTH_DATE

    /**
     * This starter as an event, titled [title] (the platform's localized name), relative to
     * [today]. `null` when it needs a [birthday] and there is none.
     */
    public fun event(title: String, today: LocalDate, birthday: CalendarDate?): Event? {
        val date =
            when (this) {
                BIRTHDAY,
                BIRTH_DATE -> birthday ?: return null
                NEXT_NEW_YEAR -> CalendarDate(today.year + 1, 1, 1)
                NEXT_MONTH ->
                    CalendarDate.of(
                        LocalDate(today.year, today.month, 1).plus(DatePeriod(months = 1))
                    )
            }
        return Event(
            id = EventId(id),
            title = title,
            date = date,
            reminders =
                if (this == BIRTHDAY) BIRTHDAY_REMINDER_DAYS.map { Reminder(it, TimeOfDay.MORNING) }
                else emptyList(),
            repeat =
                when (this) {
                    BIRTHDAY,
                    NEXT_NEW_YEAR -> RepeatRule.YEARLY
                    NEXT_MONTH -> RepeatRule.MONTHLY
                    BIRTH_DATE -> RepeatRule.NEVER
                },
            category = if (needsBirthday) EventCategory.BIRTHDAY else EventCategory.IMPORTANT,
            icon =
                when (this) {
                    BIRTHDAY -> EventIcon.BIRTHDAY_CAKE
                    BIRTH_DATE -> EventIcon.HEART
                    NEXT_NEW_YEAR -> EventIcon.STAR
                    NEXT_MONTH -> EventIcon.CALENDAR
                },
        )
    }

    private companion object {
        /** A week before, the day before and on the day, at 9:00. */
        val BIRTHDAY_REMINDER_DAYS = listOf(7, 1, 0)
    }
}
