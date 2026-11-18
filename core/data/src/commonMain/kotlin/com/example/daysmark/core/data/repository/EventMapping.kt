package com.example.daysmark.core.data.repository

import com.example.daysmark.core.data.database.EventEntity
import com.example.daysmark.core.data.database.EventWithReminders
import com.example.daysmark.core.data.database.ReminderEntity
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventIcon
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.Reminder
import com.example.daysmark.core.domain.model.RepeatRule
import com.example.daysmark.core.domain.model.TimeOfDay

internal fun EventWithReminders.toDomain(): Event =
    with(event) {
        Event(
            id = EventId(id),
            title = title,
            date = CalendarDate(year, month, day),
            repeat = RepeatRule.fromStorageKey(repeatRule),
            category = EventCategory.fromStorageKey(category),
            icon = EventIcon.fromStorageKey(icon),
            time =
                if (timeHour != null && timeMinute != null) TimeOfDay(timeHour, timeMinute)
                else null,
            endDate =
                if (endYear != null && endMonth != null && endDay != null) {
                    CalendarDate(endYear, endMonth, endDay)
                } else {
                    null
                },
            reminders =
                reminders
                    .sortedBy { it.position }
                    .map { Reminder(it.daysBefore, TimeOfDay(it.hour, it.minute)) },
        )
    }

internal fun Event.toEntity(): EventEntity =
    EventEntity(
        id = id.value,
        title = title,
        year = date.year,
        month = date.month,
        day = date.day,
        repeatRule = repeat.storageKey,
        category = category.storageKey,
        icon = icon.storageKey,
        timeHour = time?.hour,
        timeMinute = time?.minute,
        endYear = endDate?.year,
        endMonth = endDate?.month,
        endDay = endDate?.day,
    )

internal fun Event.reminderEntities(): List<ReminderEntity> =
    reminders.mapIndexed { position, reminder ->
        ReminderEntity(
            id.value,
            position,
            reminder.daysBefore,
            reminder.time.hour,
            reminder.time.minute,
        )
    }
