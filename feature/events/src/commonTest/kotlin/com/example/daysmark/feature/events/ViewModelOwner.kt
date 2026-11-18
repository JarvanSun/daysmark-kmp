package com.example.daysmark.feature.events

import com.rickclephas.kmp.observableviewmodel.InternalKMPObservableViewModelApi
import com.rickclephas.kmp.observableviewmodel.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * The lifetime of the ViewModels a test creates, as a screen's owner would manage it: Main is a
 * test dispatcher while the test runs, and every ViewModel passed to [own] is cleared afterwards
 * (cancelling its scope and collectors), so no test leaks work into the next.
 *
 * [immediate] runs coroutines as soon as they are started, for tests about resulting state. Tests
 * about ordering and lifecycle use `false` and advance the scheduler explicitly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelOwner(immediate: Boolean = true) {
    val main: TestDispatcher =
        if (immediate) UnconfinedTestDispatcher() else StandardTestDispatcher()
    private val owned = mutableListOf<ViewModel>()

    fun setUp() = Dispatchers.setMain(main)

    // clear() is what a ViewModel's owner calls when the screen is gone for good (Android's
    // ViewModelStore, SwiftUI's @StateViewModel); tests stand in for that owner.
    @OptIn(InternalKMPObservableViewModelApi::class)
    fun tearDown() {
        owned.forEach { it.clear() }
        owned.clear()
        Dispatchers.resetMain()
    }

    fun <T : ViewModel> own(viewModel: T): T = viewModel.also { owned += it }
}
