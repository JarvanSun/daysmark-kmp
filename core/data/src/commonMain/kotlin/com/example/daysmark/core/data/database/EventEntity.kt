package com.example.daysmark.core.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * One event. Dates are year, month and day, stored as entered; enums are stored by their stable
 * `storageKey`, never by ordinal or name (docs/spec/architecture.md).
 */
@Entity(tableName = "events")
internal data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val repeatRule: String,
    val category: String,
    val icon: String,
    /** Both set or both null: an all-day event has no time. */
    val timeHour: Int?,
    val timeMinute: Int?,
    /** All three set or all null: a single day has no end. */
    val endYear: Int?,
    val endMonth: Int?,
    val endDay: Int?,
)

/** One reminder of an event, in the order the user added them. Deleted with its event. */
@Entity(
    tableName = "reminders",
    primaryKeys = ["eventId", "position"],
    foreignKeys =
        [
            ForeignKey(
                entity = EventEntity::class,
                parentColumns = ["id"],
                childColumns = ["eventId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("eventId")],
)
internal data class ReminderEntity(
    val eventId: String,
    val position: Int,
    val daysBefore: Int,
    val hour: Int,
    val minute: Int,
)

internal data class EventWithReminders(
    @Embedded val event: EventEntity,
    @Relation(parentColumn = "id", entityColumn = "eventId") val reminders: List<ReminderEntity>,
)

/**
 * The one pinned event. "At most one pin" holds by construction: `slot` is the key and the only
 * write ([PinDao.setPinned]) always uses slot 0. The foreign key makes "the pin points at a stored
 * event" a schema fact.
 */
@Entity(
    tableName = "pinned_event",
    foreignKeys =
        [
            ForeignKey(
                entity = EventEntity::class,
                parentColumns = ["id"],
                childColumns = ["eventId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("eventId")],
)
internal data class PinnedEventEntity(@PrimaryKey val slot: Int = 0, val eventId: String)

/** Present once onboarding is done; when, for support and future migrations. One row (`slot` 0). */
@Entity(tableName = "onboarding")
internal data class OnboardingEntity(@PrimaryKey val slot: Int = 0, val completedAtMillis: Long)
