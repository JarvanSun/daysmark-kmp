import SwiftUI
import UserNotifications

/// Notifications, how to add the widget, and the version. All of it is system state, so this
/// screen has no shared ViewModel; it re-reads the permission whenever the app comes back.
struct SettingsView: View {
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.openURL) private var openURL
    @State private var permission: UNAuthorizationStatus?

    var body: some View {
        List {
            Section("settings.notifications.title") {
                LabeledContent("settings.notifications.permission") {
                    Text(permissionLabel)
                }
                switch permission {
                case .denied:
                    Button("settings.notifications.openSettings") {
                        if let url = URL(string: UIApplication.openNotificationSettingsURLString) {
                            openURL(url)
                        }
                    }
                case .notDetermined:
                    Button("settings.notifications.enable") { Task { await requestPermission() } }
                default:
                    EmptyView()
                }
            }
            Section("settings.widget.title") {
                Text("settings.widget.steps")
                    .foregroundStyle(.daysmark(.textSecondary))
            }
            Section {
                LabeledContent("settings.about.version", value: Self.version)
            }
        }
        .scrollContentBackground(.hidden)
        .background(.daysmark(.canvas))
        .navigationTitle("settings.title")
        .navigationBarTitleDisplayMode(.inline)
        .task(id: scenePhase) {
            if scenePhase == .active { await refreshPermission() }
        }
    }

    private var permissionLabel: LocalizedStringKey {
        switch permission {
        case .denied: "settings.notifications.denied"
        case .notDetermined: "settings.notifications.notDetermined"
        case nil: " "
        default: "settings.notifications.allowed"
        }
    }

    private func refreshPermission() async {
        permission = await UNUserNotificationCenter.current().notificationSettings().authorizationStatus
    }

    private func requestPermission() async {
        _ = try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])
        await refreshPermission()
    }

    /// "0.1.0 (1)".
    private static var version: String {
        let info = Bundle.main.infoDictionary
        let short = info?["CFBundleShortVersionString"] as? String ?? "?"
        let build = info?["CFBundleVersion"] as? String ?? "?"
        return "\(short) (\(build))"
    }
}
