package com.example.daysmark.core.domain.calculation

import com.example.daysmark.core.domain.model.CalendarDate
import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.model.RepeatRule
import com.example.daysmark.core.domain.model.TimeOfDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class CountedEventTest {
    private val zone = TimeZone.of("Asia/Shanghai")

    private fun at(y: Int, m: Int, d: Int, hour: Int = 0, minute: Int = 0, second: Int = 0) =
        LocalDateTime(y, m, d, hour, minute, second).toInstant(zone)

    private fun event(
        start: CalendarDate,
        end: CalendarDate? = null,
        time: TimeOfDay? = null,
        repeat: RepeatRule = RepeatRule.NEVER,
    ) = Event(EventId("e"), "e", start, repeat = repeat, time = time, endDate = end)

    @Test
    fun aRangeCountsToItsStartThenToItsEndThenSinceItsEnd() {
        val trip = event(CalendarDate(2026, 12, 1), end = CalendarDate(2026, 12, 5))

        val before = CountedEvent.of(trip, at(2026, 11, 28), zone)
        assertEquals(RangePhase.BEFORE_START to DayCount.Future(3), before.phase to before.dayCount)

        val during = CountedEvent.of(trip, at(2026, 12, 3), zone)
        assertEquals(RangePhase.ONGOING to DayCount.Future(2), during.phase to during.dayCount)

        val lastDay = CountedEvent.of(trip, at(2026, 12, 5), zone)
        assertEquals(RangePhase.ONGOING to DayCount.Today, lastDay.phase to lastDay.dayCount)

        val after = CountedEvent.of(trip, at(2026, 12, 7), zone)
        assertEquals(RangePhase.ENDED to DayCount.Past(2), after.phase to after.dayCount)
    }

    @Test
    fun aYearlyRangeStaysCurrentUntilItsEnd() {
        val festival =
            event(
                CalendarDate(2020, 12, 30),
                end = CalendarDate(2021, 1, 2),
                repeat = RepeatRule.YEARLY,
            )

        val duringNewYear = CountedEvent.of(festival, at(2027, 1, 1), zone)

        assertEquals(LocalDate(2026, 12, 30), duringNewYear.occurrence)
        assertEquals(RangePhase.ONGOING, duringNewYear.phase)
        assertEquals(DayCount.Future(1), duringNewYear.dayCount)
    }

    @Test
    fun aTimedEventCountsDaysBeforeItsDay() {
        val dinner = event(CalendarDate(2026, 11, 20), time = TimeOfDay(19, 30))
        assertEquals(
            DayCount.Future(2),
            CountedEvent.of(dinner, at(2026, 11, 18, 23, 0), zone).dayCount,
        )
    }

    @Test
    fun onItsDayATimedEventCountsHoursRoundedUp() {
        val dinner = event(CalendarDate(2026, 11, 18), time = TimeOfDay(19, 30))
        assertEquals(
            DayCount.Hours(3, isFuture = true),
            CountedEvent.of(dinner, at(2026, 11, 18, 16, 45), zone).dayCount,
        )
    }

    @Test
    fun inTheLastHourItCountsMinutesAndAtTheMomentItIsNow() {
        val dinner = event(CalendarDate(2026, 11, 18), time = TimeOfDay(19, 30))
        assertEquals(
            DayCount.Minutes(15, isFuture = true),
            CountedEvent.of(dinner, at(2026, 11, 18, 19, 15), zone).dayCount,
        )
        assertEquals(
            DayCount.Now,
            CountedEvent.of(dinner, at(2026, 11, 18, 19, 30, 20), zone).dayCount,
        )
        // Once past, it rounds down: an hour and a half ago is "1 hour ago", never "2".
        assertEquals(
            DayCount.Hours(1, isFuture = false),
            CountedEvent.of(dinner, at(2026, 11, 18, 21, 0), zone).dayCount,
        )
    }

    @Test
    fun aPassedMomentNeverOverstatesHowLongAgoItWas() {
        val dinner = event(CalendarDate(2026, 11, 18), time = TimeOfDay(19, 0))
        assertEquals(
            DayCount.Hours(1, isFuture = false),
            CountedEvent.of(dinner, at(2026, 11, 18, 20, 1), zone).dayCount,
        )
        assertEquals(
            DayCount.Minutes(1, isFuture = false),
            CountedEvent.of(dinner, at(2026, 11, 18, 19, 1, 59), zone).dayCount,
        )
    }

    @Test
    fun aTimedRangeHasNotStartedBeforeItsStartTimeOnItsFirstDay() {
        val festival =
            event(
                CalendarDate(2026, 11, 18),
                end = CalendarDate(2026, 11, 20),
                time = TimeOfDay(19, 0),
            )

        val morning = CountedEvent.of(festival, at(2026, 11, 18, 9, 0), zone)
        assertEquals(RangePhase.BEFORE_START, morning.phase)
        assertEquals(DayCount.Hours(10, isFuture = true), morning.dayCount)

        val evening = CountedEvent.of(festival, at(2026, 11, 18, 20, 0), zone)
        assertEquals(RangePhase.ONGOING, evening.phase)
        assertEquals(DayCount.Future(2), evening.dayCount)
    }

    @Test
    fun theTimeIsTheSameWallClockTimeInEveryZone() {
        val dinner = event(CalendarDate(2026, 11, 18), time = TimeOfDay(19, 30))
        val berlin = TimeZone.of("Europe/Berlin")
        val berlinAfternoon = LocalDateTime(2026, 11, 18, 17, 30).toInstant(berlin)
        assertEquals(
            DayCount.Hours(2, isFuture = true),
            CountedEvent.of(dinner, berlinAfternoon, berlin).dayCount,
        )
    }
}
