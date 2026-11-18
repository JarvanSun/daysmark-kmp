import Foundation

/// How far away an event is, worded and coloured the same everywhere (docs/spec/product.md,
/// "Relative time"). The app maps the shared Kotlin `DayCount` onto it; the widget, which has no
/// Kotlin, maps its snapshot onto it.
enum DayDistance: Equatable {
    case today
    case now
    case future(days: Int)
    case past(days: Int)
    case hours(Int, isFuture: Bool)
    case minutes(Int, isFuture: Bool)

    var phrase: String {
        switch self {
        case .today: String(localized: "phrase.today")
        case .now: String(localized: "phrase.now")
        case .future(1): String(localized: "phrase.tomorrow")
        case let .future(days): String(localized: "phrase.future.days \(days)")
        case .past(1): String(localized: "phrase.yesterday")
        case let .past(days): String(localized: "phrase.past.days \(days)")
        case let .hours(hours, isFuture): Self.relative(DateComponents(hour: isFuture ? hours : -hours))
        case let .minutes(minutes, isFuture): Self.relative(DateComponents(minute: isFuture ? minutes : -minutes))
        }
    }

    /// Upcoming and past differ in colour and in wording, never in colour alone.
    var tint: DaysmarkColor {
        switch self {
        case .past, .hours(_, false), .minutes(_, false): .past
        default: .upcoming
        }
    }

    var countsDown: Bool { tint == .upcoming }

    /// "in 3 hours", "3小时后": Foundation's own wording for each language.
    private static func relative(_ components: DateComponents) -> String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full
        return formatter.localizedString(from: components)
    }
}

/// Where today falls in an event that has an end date.
enum RangePlacement: String {
    case point
    case beforeStart
    case ongoing
    case ended

    /// Places a distance phrase in the range: "Starts in 3 days", "明天结束".
    func place(_ phrase: String) -> String {
        let embedded = phrase.prefix(1).lowercased() + phrase.dropFirst()
        return switch self {
        case .point: phrase
        case .beforeStart: String(localized: "phrase.range.starts \(embedded)")
        case .ongoing: String(localized: "phrase.range.ends \(embedded)")
        case .ended: String(localized: "phrase.range.ended \(embedded)")
        }
    }
}
