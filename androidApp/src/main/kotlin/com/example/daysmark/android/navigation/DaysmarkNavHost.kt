package com.example.daysmark.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.daysmark.android.detail.DetailScreen
import com.example.daysmark.android.editor.EditorEntry
import com.example.daysmark.android.home.HomeScreen
import com.example.daysmark.android.settings.SettingsScreen
import com.example.daysmark.android.share.ShareScreen
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.feature.events.editor.EditorTarget
import com.example.daysmark.shared.AppGraph

/** The back stack and every screen; each screen's ViewModel lives as long as its entry. */
@Composable
fun DaysmarkNavHost(graph: AppGraph, request: LaunchRequest?, onRequestHandled: () -> Unit) {
    val backStack = rememberNavBackStack(Route.Home)
    val navigator = Navigator(backStack)
    // A tapped reminder or widget opens its event (or a new one) on top of Home.
    LaunchedEffect(request) {
        when (request) {
            is LaunchRequest.OpenEvent -> navigator.showOnly(EventId(request.eventId))
            LaunchRequest.NewEvent -> navigator.showNewEventOnly()
            null -> return@LaunchedEffect
        }
        onRequestHandled()
    }
    NavDisplay(
        backStack = backStack,
        onBack = { navigator.back() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<Route.Home> { HomeScreen(viewModel { graph.homeViewModel }, navigator) }
                entry<Route.NewEvent> { route ->
                    val category = EventCategory.fromStorageKey(route.categoryKey)
                    EditorEntry(graph, EditorTarget.New(category), onClose = { navigator.back() })
                }
                entry<Route.EditEvent> { route ->
                    EditorEntry(
                        graph,
                        EditorTarget.Existing(EventId(route.eventId)),
                        onClose = { navigator.back() },
                    )
                }
                entry<Route.Detail> { route ->
                    DetailScreen(
                        viewModel { graph.detailViewModelFactory.create(EventId(route.eventId)) },
                        onBack = { navigator.back() },
                        onEdit = navigator::edit,
                        onShare = navigator::share,
                    )
                }
                entry<Route.Share> { route ->
                    ShareScreen(
                        viewModel { graph.shareViewModelFactory.create(EventId(route.eventId)) },
                        onDone = { navigator.back() },
                    )
                }
                entry<Route.Settings> { SettingsScreen(onBack = { navigator.back() }) }
            },
    )
}
