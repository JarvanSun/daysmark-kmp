package com.example.daysmark.feature.events.home

import app.cash.turbine.test
import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.RepeatRule
import com.example.daysmark.core.testing.FakeEventRepository
import com.example.daysmark.core.testing.FixedWallClock
import com.example.daysmark.core.testing.RecordingErrorReporter
import com.example.daysmark.feature.events.ViewModelOwner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val reporter = RecordingErrorReporter()
    private val repository = FakeEventRepository()
    private val clock = FixedWallClock(LocalDate(2026, 11, 18))

    private val owner = ViewModelOwner()

    @BeforeTest fun setUp() = owner.setUp()

    @AfterTest fun tearDown() = owner.tearDown()

    private fun event(
        id: String,
        year: Int,
        month: Int,
        day: Int,
        category: EventCategory = EventCategory.IMPORTANT,
        repeat: RepeatRule = RepeatRule.NEVER,
    ) =
        Event(
            EventId(id),
            id,
            CalendarDate(year, month, day),
            repeat,
            category,
        )

    private fun viewModel() = owner.own(HomeViewModel(repository, clock, reporter))

    @Test
    fun withoutEventsHomeInvitesTheFirstOne() = runTest {
        viewModel().uiState.test {
            val content = awaitItem() as HomeUiState.Content
            assertEquals(HomePlaceholder.NO_EVENTS, content.placeholder)
            assertFalse(content.showsOverview)
            assertNull(content.focus)
        }
    }

    @Test
    fun listsEventsInOrderWithTheNearestAsFocus() = runTest {
        repository.events.value =
            listOf(
                event("past", 2026, 11, 10),
                event("far", 2026, 12, 25),
                event("near", 2026, 11, 20),
            )

        viewModel().uiState.test {
            val content = awaitItem() as HomeUiState.Content
            assertEquals(listOf("near", "far", "past"), content.rows.map { it.title })
            assertEquals("near", content.focus?.title)
            assertEquals(LandscapeCounts(past = 1, future = 2), content.landscape)
        }
    }

    @Test
    fun aYearlyBirthdayCountsToItsNextOccurrence() = runTest {
        repository.events.value =
            listOf(event("birthday", 1998, 10, 25, repeat = RepeatRule.YEARLY))

        viewModel().uiState.test {
            val row = (awaitItem() as HomeUiState.Content).rows.single()
            assertEquals(DayCount.Future(341), row.dayCount)
            assertEquals(CalendarDate(2027, 10, 25), row.date)
            assertTrue(row.showsYear)
        }
    }

    @Test
    fun aDateThisYearHidesItsYear() = runTest {
        repository.events.value = listOf(event("soon", 2026, 12, 1))

        viewModel().uiState.test {
            assertFalse((awaitItem() as HomeUiState.Content).rows.single().showsYear)
        }
    }

    @Test
    fun selectingACategoryFiltersTheListButNotTheFocus() = runTest {
        repository.events.value =
            listOf(event("near", 2026, 11, 20), event("cake", 2026, 12, 1, EventCategory.BIRTHDAY))
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onAction(HomeAction.SelectCategory(EventCategory.BIRTHDAY))
            val content = awaitItem() as HomeUiState.Content
            assertEquals(listOf("cake"), content.rows.map { it.title })
            assertEquals("near", content.focus?.title)
            assertEquals(EventCategory.BIRTHDAY, content.filters.single { it.isSelected }.category)
        }
    }

    @Test
    fun anEmptyCategoryOffersAResetThatShowsEverythingAgain() = runTest {
        repository.events.value = listOf(event("near", 2026, 11, 20))
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onAction(HomeAction.SelectCategory(EventCategory.ANNIVERSARY))
            assertEquals(
                HomePlaceholder.NO_MATCHES,
                (awaitItem() as HomeUiState.Content).placeholder,
            )
            viewModel.onAction(HomeAction.ResetFilters)
            val reset = awaitItem() as HomeUiState.Content
            assertNull(reset.placeholder)
            assertEquals(null, reset.filters.single { it.isSelected }.category)
        }
    }

    @Test
    fun pinningMakesAnEventTheFocusAndPinningAgainUnpinsIt() = runTest {
        repository.events.value = listOf(event("near", 2026, 11, 20), event("far", 2026, 12, 25))
        val viewModel = viewModel()

        viewModel.onAction(HomeAction.TogglePin(EventId("far")))
        assertEquals(EventId("far"), repository.pinned.value)

        viewModel.onAction(HomeAction.TogglePin(EventId("far")))
        assertNull(repository.pinned.value)
    }

    @Test
    fun deleteRemovesTheEvent() = runTest {
        repository.events.value = listOf(event("near", 2026, 11, 20))

        viewModel().onAction(HomeAction.Delete(EventId("near")))

        assertEquals(emptyList(), repository.events.value)
    }

    @Test
    fun aTimedEventLaterTodayStillStandsInTheLandscape() {
        val landscape =
            LandscapeCounts.of(
                listOf(
                    DayCount.Hours(3, isFuture = true),
                    DayCount.Minutes(5, isFuture = false),
                    DayCount.Past(2),
                )
            )
        assertEquals(LandscapeCounts(past = 1, future = 2), landscape)
    }
}
