import Foundation

/// Links the widget opens the app with.
enum DeepLink: Equatable {
    case event(id: String)
    case newEvent

    private static let scheme = "daysmark-kmp"

    var url: URL {
        switch self {
        case let .event(id): URL(string: "\(Self.scheme)://event/\(id)")!
        case .newEvent: URL(string: "\(Self.scheme)://new")!
        }
    }

    init?(_ url: URL) {
        guard url.scheme == Self.scheme else { return nil }
        switch url.host() {
        case "event":
            guard let id = url.pathComponents.dropFirst().first else { return nil }
            self = .event(id: id)
        case "new": self = .newEvent
        default: return nil
        }
    }
}
