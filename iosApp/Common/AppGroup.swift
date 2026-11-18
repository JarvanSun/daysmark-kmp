import Foundation

/// The container the app and the widget share. The app writes the widget's timeline there
/// (`WidgetSnapshot` in Kotlin); the widget only reads it.
enum AppGroup {
    static let identifier = "group.com.example.daysmark.kmp"

    static var widgetTimelineURL: URL? {
        FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: identifier)?
            .appending(path: "widget-timeline.json")
    }
}
