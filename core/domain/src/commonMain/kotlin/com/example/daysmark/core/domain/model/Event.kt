package com.example.daysmark.core.domain.model

/**
 * An event's identity. A data class, not a `value class`: value classes reach Swift as `Any`
 * (docs/spec/ios-interop.md).
 */
public data class EventId(val value: String)

/**
 * One important date. With an [endDate] it is a range (a trip, a festival), counted to its start,
 * then to its end. With a [time] it counts hours and minutes on its day.
 */
public data class Event(
    val id: EventId,
    val title: String,
    val date: CalendarDate,
    val repeat: RepeatRule = RepeatRule.NEVER,
    val category: EventCategory = EventCategory.IMPORTANT,
    val icon: EventIcon = EventIcon.CALENDAR,
    val time: TimeOfDay? = null,
    val endDate: CalendarDate? = null,
    val reminders: List<Reminder> = emptyList(),
) {
    init {
        require(endDate == null || endDate.toLocalDate() >= date.toLocalDate()) {
            "A range can't end before it starts"
        }
    }
}
