package com.example.daysmark.core.domain.model

/**
 * The three built-in categories. A category only groups and filters: it never changes how an event
 * counts, looks or reminds. [storageKey] is what the database stores and must never change.
 */
public enum class EventCategory(public val storageKey: String) {
    IMPORTANT("important"),
    BIRTHDAY("birthday"),
    ANNIVERSARY("anniversary");

    public companion object {
        /** Unknown keys (from a newer version) fall back to [IMPORTANT], never fail. */
        public fun fromStorageKey(key: String): EventCategory =
            entries.firstOrNull { it.storageKey == key } ?: IMPORTANT
    }
}
