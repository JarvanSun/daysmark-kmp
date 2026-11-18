package com.example.daysmark.android.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventId

/** The navigation intents screens use; they never touch the back stack themselves. */
class Navigator(private val backStack: NavBackStack<NavKey>) {
    fun openDetail(id: EventId) {
        backStack.add(Route.Detail(id.value))
    }

    fun newEvent(category: EventCategory) {
        backStack.add(Route.NewEvent(category.storageKey))
    }

    fun edit(id: EventId) {
        backStack.add(Route.EditEvent(id.value))
    }

    fun share(id: EventId) {
        backStack.add(Route.Share(id.value))
    }

    fun openSettings() {
        backStack.add(Route.Settings)
    }

    /** Home with this event's detail on top, whatever was open before. */
    fun showOnly(id: EventId) {
        backStack.retainAll { it == Route.Home }
        backStack.add(Route.Detail(id.value))
    }

    fun showNewEventOnly() {
        backStack.retainAll { it == Route.Home }
        newEvent(EventCategory.IMPORTANT)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
}
