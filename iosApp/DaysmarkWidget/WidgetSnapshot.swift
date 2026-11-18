import Foundation

/// The timeline the app writes for the widget: the Swift mirror of Kotlin's `WidgetSnapshot`
/// (shared/.../widget/WidgetSnapshot.kt). Change both together and raise `version`.
struct WidgetSnapshot: Decodable {
    static let version = 2

    let version: Int
    let entries: [Entry]

    struct Entry: Decodable {
        let startsAtMillis: Int64
        let focus: Focus?

        var startsAt: Date { Date(timeIntervalSince1970: TimeInterval(startsAtMillis) / 1000) }
    }

    struct Focus: Decodable, Equatable {
        let id: String
        let title: String
        let icon: String
        let count: Count
        /// An unknown phase makes the file invalid, as in Kotlin.
        let placement: RangePlacement

        private enum CodingKeys: String, CodingKey { case id, title, icon, count, phase }

        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            id = try container.decode(String.self, forKey: .id)
            title = try container.decode(String.self, forKey: .title)
            icon = try container.decode(String.self, forKey: .icon)
            count = try container.decode(Count.self, forKey: .count)
            let phase = try container.decode(String.self, forKey: .phase)
            guard let placement = RangePlacement(rawValue: phase) else {
                throw DecodingError.dataCorruptedError(
                    forKey: .phase, in: container, debugDescription: "Unknown phase \(phase)")
            }
            self.placement = placement
        }

        init(id: String, title: String, icon: String, count: Count, placement: RangePlacement) {
            (self.id, self.title, self.icon, self.count, self.placement) = (id, title, icon, count, placement)
        }

        var distance: DayDistance { count.distance }
        var phrase: String { placement.place(distance.phrase) }
        var artwork: IconArtwork { IconArtwork(storageKey: icon) }
    }

    /// Every field is required, and an unknown kind is an invalid file, exactly as in Kotlin
    /// (resources/fixtures/widget-timeline-invalid).
    struct Count: Decodable, Equatable {
        let distance: DayDistance

        private enum CodingKeys: String, CodingKey { case kind, value, future }

        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            let kind = try container.decode(String.self, forKey: .kind)
            let value = try container.decode(Int.self, forKey: .value)
            let future = try container.decode(Bool.self, forKey: .future)
            distance =
                switch kind {
                case "future": .future(days: value)
                case "past": .past(days: value)
                case "hours": .hours(value, isFuture: future)
                case "minutes": .minutes(value, isFuture: future)
                case "now": .now
                case "today": .today
                default:
                    throw DecodingError.dataCorruptedError(
                        forKey: .kind, in: container, debugDescription: "Unknown count kind \(kind)")
                }
        }

        init(distance: DayDistance) { self.distance = distance }
    }

    /// The file in the App Group, or `nil` when the app hasn't written one this version can read.
    static func load() -> WidgetSnapshot? {
        guard let url = AppGroup.widgetTimelineURL,
            let data = try? Data(contentsOf: url),
            let snapshot = try? JSONDecoder().decode(WidgetSnapshot.self, from: data),
            snapshot.version == version
        else { return nil }
        return snapshot
    }
}
