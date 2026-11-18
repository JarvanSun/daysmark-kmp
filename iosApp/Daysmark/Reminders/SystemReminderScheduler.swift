import OSLog
import Shared
import UserNotifications

/// Schedules the shared reminder plan as local notifications. Every request it owns starts with
/// `daysmark.`, so replacing the plan never touches anything else.
final class SystemReminderScheduler: NSObject, ReminderScheduler {
    nonisolated static let eventIDKey = "eventID"
    private nonisolated static let prefix = "daysmark."
    private let logger = Logger(subsystem: "com.example.daysmark.kmp", category: "reminders")

    /// Returns once the system holds exactly these requests; the shared sync calls it one plan at a
    /// time. A refusal is logged and thrown, and the sync tries again with its next plan.
    func replaceAll(reminders: [PlannedReminder]) async throws {
        let requests = reminders.map(Self.request)
        let center = UNUserNotificationCenter.current()
        let ours = await center.pendingNotificationRequests().map(\.identifier).filter {
            $0.hasPrefix(Self.prefix)
        }
        center.removePendingNotificationRequests(withIdentifiers: ours)
        do {
            for request in requests { try await center.add(request) }
        } catch {
            logger.error("Couldn't schedule reminders: \(error.localizedDescription)")
            throw error
        }
    }

    private static func request(_ reminder: PlannedReminder) -> UNNotificationRequest {
        let content = UNMutableNotificationContent()
        content.title = reminder.title
        content.body = reminder.phase.place(reminder.dayCount.phrase)
        content.sound = .default
        content.userInfo = [eventIDKey: reminder.eventId.value]
        // Wall-clock components without a zone: it fires at that local time wherever the user is.
        let components = DateComponents(
            year: Int(reminder.date.year), month: Int(reminder.date.month), day: Int(reminder.date.day),
            hour: Int(reminder.time.hour), minute: Int(reminder.time.minute))
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        return UNNotificationRequest(identifier: prefix + reminder.id, content: content, trigger: trigger)
    }
}
