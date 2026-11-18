import OSLog
import Shared

/// Records failures the app recovers from in the system log, with their cause. Called from
/// Kotlin's background threads, so it is not main-actor isolated (the project's default):
/// `Logger` is thread-safe and this keeps no state.
nonisolated final class LogErrorReporter: ErrorReporter, Sendable {
    private let logger = Logger(subsystem: "com.example.daysmark.kmp", category: "failures")

    func report(failure: KotlinThrowable, context: String) {
        logger.error("\(context): \(failure.description())")
    }
}
