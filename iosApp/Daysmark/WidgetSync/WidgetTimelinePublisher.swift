import OSLog
import Shared
import WidgetKit

/// Writes the shared widget timeline into the App Group and asks WidgetKit to reload it. Returns
/// once the file is written; a failure is logged and thrown, and the sync tries again with its
/// next timeline.
final class WidgetTimelinePublisher: WidgetPublisher {
    private let logger = Logger(subsystem: "com.example.daysmark.kmp", category: "widget")

    func publish(timeline: WidgetTimeline) async throws {
        guard let url = AppGroup.widgetTimelineURL else {
            logger.fault("The App Group \(AppGroup.identifier) is missing from the entitlements")
            throw CocoaError(.fileNoSuchFile)
        }
        let data = Data(WidgetSnapshot.shared.encode(timeline: timeline).utf8)
        do {
            // Off the main thread: the file write is the only slow part.
            try await Task.detached(priority: .utility) { try data.write(to: url, options: .atomic) }.value
        } catch {
            logger.error("Couldn't write the widget timeline: \(error.localizedDescription)")
            throw error
        }
        WidgetCenter.shared.reloadAllTimelines()
    }
}
