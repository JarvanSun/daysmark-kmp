import Shared
import Testing

@testable import Daysmark

/// Kotlin's sealed types and enums can't be switched over exhaustively in Swift. These tests go
/// through every case the shared code defines (enums by their `entries`, sealed types through
/// `SwiftTestExamples`, which Kotlin keeps complete), so a case added in Kotlin but not mapped here fails
/// (through `unexpectedCase`) instead of quietly showing as another value.
@MainActor
struct SharedMappingTests {
    @Test func everyDayCountMapsToItsOwnDistance() {
        // SwiftTestExamples has an example of every DayCount case (enforced in Kotlin).
        let distances = SwiftTestExamples.shared.dayCounts.map(\.distance)
        #expect(distances.count == SwiftTestExamples.shared.dayCounts.count)
        #expect(Set(distances.map { "\($0)" }).count == distances.count)
        #expect(
            distances == [
                .today, .now, .future(days: 3), .past(days: 4), .hours(2, isFuture: true),
                .minutes(5, isFuture: false),
            ])
    }

    @Test func everyRangePhaseIsPlacedDifferently() {
        let placed = RangePhase.entries.map { $0.place("in 3 days") }
        #expect(Set(placed).count == RangePhase.entries.count)
    }

    @Test func everyStarterHasItsOwnTitle() {
        let titles = StarterEvent.entries.map(\.title)
        #expect(Set(titles).count == StarterEvent.entries.count)
    }

    @Test func everyIconHasItsOwnDrawing() {
        let files = EventIcon.entries.map { IconArtwork(storageKey: $0.storageKey).fileName }
        #expect(Set(files).count == EventIcon.entries.count)
    }

    @Test func everyOnboardingStepAndActionIsMapped() {
        // Reaching unexpectedCase would stop here in a debug build.
        _ = OnboardingStep.entries.map(\.title)
        _ = OnboardingPrimaryAction.entries.map(\.title)
    }

    @Test func gregorianDatesRoundTripWhateverTheSystemCalendar() {
        let day = CalendarDate(year: 2028, month: 2, day: 29)
        #expect(CalendarDate.from(day.date) == day)
        #expect(day.adding(days: 1) == CalendarDate(year: 2028, month: 3, day: 1))
    }
}
