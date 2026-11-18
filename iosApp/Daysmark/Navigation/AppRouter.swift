import Shared
import SwiftUI

/// Where the app is: the pushed screens and the one presented sheet. Views ask the router to
/// navigate; they never hold navigation state themselves.
@Observable
final class AppRouter {
    enum Route: Hashable {
        case detail(eventID: String)
        case settings
    }

    enum Sheet: Identifiable {
        case newEvent(category: EventCategory)
        case edit(eventID: String)
        case share(eventID: String)

        var id: String {
            switch self {
            case let .newEvent(category): "new-\(category.name)"
            case let .edit(eventID): "edit-\(eventID)"
            case let .share(eventID): "share-\(eventID)"
            }
        }
    }

    var path: [Route] = []
    var sheet: Sheet?

    func openDetail(_ id: EventId) { path.append(.detail(eventID: id.value)) }
    /// Opens an event from outside the app (a notification, the widget): over Home, nothing else.
    func show(_ id: EventId) {
        sheet = nil
        path = [.detail(eventID: id.value)]
    }

    func openSettings() { path.append(.settings) }
    func newEvent(in category: EventCategory) { sheet = .newEvent(category: category) }
    func edit(_ id: EventId) { sheet = .edit(eventID: id.value) }
    func share(_ id: EventId) { sheet = .share(eventID: id.value) }
}
