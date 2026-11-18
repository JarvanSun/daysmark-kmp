package com.example.daysmark.feature.events.share

import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.calculation.RangePhase
import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventCategory
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.RepeatRule
import com.example.daysmark.core.domain.model.TimeOfDay
import com.example.daysmark.core.testing.FakeEventRepository
import com.example.daysmark.core.testing.FixedWallClock
import com.example.daysmark.core.testing.RecordingErrorReporter
import com.example.daysmark.feature.events.ViewModelOwner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ShareViewModelTest {
    private val reporter = RecordingErrorReporter()
    private val repository = FakeEventRepository()
    private val clock = FixedWallClock(LocalDate(2026, 10, 7))

    private val owner = ViewModelOwner()

    @BeforeTest fun setUp() = owner.setUp()

    @AfterTest fun tearDown() = owner.tearDown()

    @Test
    fun theCardShowsTheNextOccurrenceAndWhenItWasTaken() {
        repository.events.value =
            listOf(
                Event(
                    EventId("mia"),
                    "Mia's birthday",
                    CalendarDate(1998, 10, 25),
                    repeat = RepeatRule.YEARLY,
                    category = EventCategory.BIRTHDAY,
                )
            )
        val viewModel = owner.own(ShareViewModel(EventId("mia"), repository, clock, reporter))

        val content = viewModel.uiState.value as ShareUiState.Content
        assertEquals(DayCount.Future(18), content.dayCount)
        assertEquals(RangePhase.POINT, content.phase)
        assertEquals(CalendarDate(2026, 10, 25), content.start)
        assertEquals(CalendarDate(2026, 10, 7), content.asOfDate)
        assertEquals(TimeOfDay(0, 0), content.asOfTime)
        assertFalse(content.includesCategory)

        viewModel.onAction(ShareAction.SetIncludesCategory(true))
        assertTrue((viewModel.uiState.value as ShareUiState.Content).includesCategory)
    }

    @Test
    fun aDeletedEventCannotBeShared() {
        assertEquals(
            ShareUiState.Missing,
            owner.own(ShareViewModel(EventId("gone"), repository, clock, reporter)).uiState.value,
        )
    }
}
