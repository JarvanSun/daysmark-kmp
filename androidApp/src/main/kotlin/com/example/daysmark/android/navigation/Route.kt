package com.example.daysmark.android.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Every screen, as a typed back-stack entry. Saved across process death. */
@Serializable
sealed interface Route : NavKey {
    @Serializable data object Home : Route

    @Serializable data class Detail(val eventId: String) : Route

    /** The editor for a new event in the category with [categoryKey]. */
    @Serializable data class NewEvent(val categoryKey: String) : Route

    @Serializable data class EditEvent(val eventId: String) : Route

    @Serializable data class Share(val eventId: String) : Route

    @Serializable data object Settings : Route
}
