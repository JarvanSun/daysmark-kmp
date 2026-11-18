/// Kotlin's sealed types and enums reach Swift as classes, so a `switch` over them can't be
/// exhaustive and needs a `default`. Every known case is listed explicitly; the `default` calls this:
/// a case added in Kotlin but not handled here stops a debug build at once (and the app-hosted
/// tests, which go through every case), and a release build falls back to `fallback`.
func unexpectedCase<T>(_ value: Any, fallback: T) -> T {
    assertionFailure("Unhandled case from the shared code: \(value)")
    return fallback
}
