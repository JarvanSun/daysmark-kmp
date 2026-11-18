import SwiftUI
import WidgetKit

/// The event in focus on Home, counted from each day. The app computes the whole timeline (now and
/// the following midnights), so the widget never decides anything itself.
struct FocusWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "focus", provider: FocusProvider()) { entry in
            FocusWidgetView(entry: entry)
        }
        .configurationDisplayName("widget.focus.name")
        .description("widget.focus.description")
        .supportedFamilies([.systemSmall, .accessoryRectangular, .accessoryInline])
    }
}

struct FocusEntry: TimelineEntry {
    let date: Date
    /// `nil` when there are no events yet.
    let focus: WidgetSnapshot.Focus?
}

struct FocusProvider: TimelineProvider {
    func placeholder(in context: Context) -> FocusEntry {
        FocusEntry(date: .now, focus: .placeholder)
    }

    func getSnapshot(in context: Context, completion: @escaping (FocusEntry) -> Void) {
        let current = timeline().entries.last { $0.date <= .now }
        completion(current ?? FocusEntry(date: .now, focus: context.isPreview ? .placeholder : nil))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<FocusEntry>) -> Void) {
        completion(timeline())
    }

    private func timeline() -> Timeline<FocusEntry> {
        guard let snapshot = WidgetSnapshot.load(), !snapshot.entries.isEmpty else {
            // The app writes the file when it starts; until then there is nothing to count.
            return Timeline(entries: [FocusEntry(date: .now, focus: nil)], policy: .never)
        }
        let entries = snapshot.entries.map { FocusEntry(date: $0.startsAt, focus: $0.focus) }
        // Past the last midnight the app planned, ask again; the app replans whenever it runs.
        let last = entries[entries.count - 1].date
        return Timeline(entries: entries, policy: last > .now ? .after(last) : .never)
    }
}

extension WidgetSnapshot.Focus {
    static let placeholder = WidgetSnapshot.Focus(
        id: "placeholder", title: "Daysmark", icon: "star",
        count: WidgetSnapshot.Count(distance: .future(days: 12)), placement: .point)
}
