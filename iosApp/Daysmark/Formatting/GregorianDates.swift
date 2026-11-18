import Foundation
import Shared

/// The app counts in the Gregorian calendar, whatever calendar the user's iPhone is set to
/// (docs/spec/product.md). Every conversion between the shared date values and Foundation dates,
/// and every written date, goes through here. `Calendar.current` would follow a Japanese or Buddhist
/// system calendar and turn 2026 into year 8 or 2569.
enum Gregorian {
    /// The Gregorian calendar in the user's locale and time zone (wall-clock values).
    static var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.locale = .current
        calendar.timeZone = .current
        return calendar
    }

    static func date(_ day: CalendarDate, at time: TimeOfDay? = nil) -> Date {
        let components = DateComponents(
            year: Int(day.year), month: Int(day.month), day: Int(day.day),
            hour: time.map { Int($0.hour) }, minute: time.map { Int($0.minute) })
        guard let date = calendar.date(from: components) else {
            // CalendarDate rejects impossible dates, so every one exists in the Gregorian calendar.
            preconditionFailure("\(day) has no Gregorian date")
        }
        return date
    }

    /// A format style that writes dates in the Gregorian calendar; add fields to it as usual.
    static func style(date: Date.FormatStyle.DateStyle = .omitted, time: Date.FormatStyle.TimeStyle = .omitted)
        -> Date.FormatStyle
    {
        Date.FormatStyle(date: date, time: time, calendar: calendar)
    }
}

extension CalendarDate {
    var date: Date { Gregorian.date(self) }

    static func from(_ date: Date) -> CalendarDate {
        let c = Gregorian.calendar.dateComponents([.year, .month, .day], from: date)
        guard let year = c.year, let month = c.month, let day = c.day else {
            preconditionFailure("The Gregorian calendar always has a year, month and day")
        }
        return CalendarDate(year: Int32(year), month: Int32(month), day: Int32(day))
    }

    func adding(days: Int) -> CalendarDate {
        guard let moved = Gregorian.calendar.date(byAdding: .day, value: days, to: date) else {
            preconditionFailure("Adding \(days) days to \(self) left the calendar")
        }
        return .from(moved)
    }
}

extension TimeOfDay {
    /// This time on a fixed day, for time pickers, which only read its hour and minute.
    var date: Date {
        guard let date = Gregorian.calendar.date(from: DateComponents(hour: Int(hour), minute: Int(minute)))
        else { preconditionFailure("\(self) is not a time of day") }
        return date
    }

    static func from(_ date: Date) -> TimeOfDay {
        let c = Gregorian.calendar.dateComponents([.hour, .minute], from: date)
        guard let hour = c.hour, let minute = c.minute else {
            preconditionFailure("The Gregorian calendar always has an hour and minute")
        }
        return TimeOfDay(hour: Int32(hour), minute: Int32(minute))
    }
}
