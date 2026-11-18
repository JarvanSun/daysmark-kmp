import SwiftUI

/// Stored data couldn't be read: the system's "content unavailable" layout with Try Again.
struct LoadFailedView: View {
    let onRetry: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label("failure.load.title", systemImage: "exclamationmark.triangle")
        } description: {
            Text("failure.load.message")
        } actions: {
            Button("failure.load.retry", action: onRetry)
                .buttonStyle(.glassProminent)
                .tint(.daysmark(.tint))
        }
    }
}

extension View {
    /// Tells the user a change couldn't be stored; the state's dismiss action clears it.
    func failureAlert(_ title: LocalizedStringKey, isPresented: Bool, onDismiss: @escaping () -> Void)
        -> some View
    {
        alert(
            title,
            isPresented: Binding(get: { isPresented }, set: { if !$0 { onDismiss() } })
        ) {
            Button("failure.ok", role: .cancel) {}
        } message: {
            Text("failure.write.message")
        }
    }
}
