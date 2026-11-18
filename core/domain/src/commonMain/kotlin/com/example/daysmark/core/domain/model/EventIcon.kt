package com.example.daysmark.core.domain.model

/**
 * An event's illustrated icon. The platforms map each case to the same artwork file. [storageKey]
 * matches the Swift Daysmark app, so imported data keeps its icons.
 */
public enum class EventIcon(public val storageKey: String) {
    STAR("star"),
    GIFT("gift"),
    HEART("heart"),
    AIRPLANE("airplane"),
    GRADUATION_CAP("graduationcap"),
    BIRTHDAY_CAKE("birthday.cake"),
    BRIEFCASE("briefcase"),
    HOUSE("house"),
    RUNNING("figure.run"),
    BOOK("book"),

    /** The default when nothing was chosen; not offered in the picker. */
    CALENDAR("calendar");

    public companion object {
        public fun fromStorageKey(key: String): EventIcon =
            entries.firstOrNull { it.storageKey == key } ?: CALENDAR
    }
}
