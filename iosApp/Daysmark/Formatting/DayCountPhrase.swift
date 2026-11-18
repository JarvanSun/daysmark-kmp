import Shared
import SwiftUI

extension DayCount {
    var distance: DayDistance {
        switch self {
        case let future as DayCountFuture: .future(days: Int(future.days))
        case let past as DayCountPast: .past(days: Int(past.days))
        case let hours as DayCountHours: .hours(Int(hours.hours), isFuture: hours.isFuture)
        case let minutes as DayCountMinutes: .minutes(Int(minutes.minutes), isFuture: minutes.isFuture)
        case is DayCountNow: .now
        case is DayCountToday: .today
        default: unexpectedCase(self, fallback: .today)
        }
    }

    /// The one localized phrase for a distance (docs/spec/product.md, "Relative time").
    var phrase: String { distance.phrase }
    var tint: DaysmarkColor { distance.tint }
    var countsDown: Bool { distance.countsDown }
}

extension RangePhase {
    /// Places a distance phrase in a range: "Starts in 3 days", "明天结束".
    func place(_ phrase: String) -> String {
        let placement: RangePlacement =
            switch self {
            case .beforeStart: .beforeStart
            case .ongoing: .ongoing
            case .ended: .ended
            case .point: .point
            default: unexpectedCase(self, fallback: .point)
            }
        return placement.place(phrase)
    }
}

extension EventRow {
    /// The phrase every surface shows for this row, placed in its range phase.
    var phrase: String { phase.place(dayCount.phrase) }

    /// The day as the user's locale writes it: "Oct 25", "Oct 25, 2027", "Oct 25, 7:30 PM".
    var formattedDate: String {
        var style = Gregorian.style().month(.abbreviated).day()
        if showsYear { style = style.year() }
        if time != nil { style = style.hour().minute() }
        return Gregorian.date(date, at: time).formatted(style)
    }

    var spokenDescription: String {
        [title, phrase, formattedDate].filter { !$0.isEmpty }.joined(separator: ", ")
    }
}
