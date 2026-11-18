package com.example.daysmark.core.domain.model

/** How an event repeats. A repeating event always shows its next occurrence. */
public enum class RepeatRule(public val storageKey: String) {
    NEVER("never"),
    MONTHLY("monthly"),
    YEARLY("yearly");

    public companion object {
        public fun fromStorageKey(key: String): RepeatRule =
            entries.firstOrNull { it.storageKey == key } ?: NEVER
    }
}
