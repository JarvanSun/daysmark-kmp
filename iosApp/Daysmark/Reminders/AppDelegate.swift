import Combine
import UIKit
import UserNotifications

/// Receives notification taps and shows notifications while the app is open.
final class AppDelegate: NSObject, ObservableObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    /// Set by the root view; a tap before it exists is kept until it does.
    @MainActor var openEvent: ((String) -> Void)? {
        didSet {
            if let pending = pendingEventID, let openEvent {
                pendingEventID = nil
                openEvent(pending)
            }
        }
    }

    @MainActor private var pendingEventID: String?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        guard let id = response.notification.request.content.userInfo[SystemReminderScheduler.eventIDKey] as? String
        else { return }
        await MainActor.run {
            if let openEvent { openEvent(id) } else { pendingEventID = id }
        }
    }
}
