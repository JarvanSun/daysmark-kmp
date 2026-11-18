# Presentation

Each screen has one ViewModel in shared Kotlin with exactly two public members (enforced by
`PresentationRulesTest`):

```kotlin
public val uiState: StateFlow<HomeUiState>   // everything the screen shows
public fun onAction(action: HomeAction)      // everything the user can do
```

- **`UiState` is one immutable value**, usually a `sealed interface` (`Loading`, `Content`,
  `Failed`). The platform switches over it and draws; it never combines flows or derives values.
- **`Action` is a `sealed interface`**, one case per intent, so a new case breaks every `when`
  that must handle it.
- **Outcomes are state**, not a separate event channel a configuration change could drop: the
  editor's `Finished`, a write failure. Purely visual steps (a delete confirmation) stay in the
  view.
- **Sharing**: read-only screens use `WhileOnScreen` (`WhileSubscribed(5_000)`), so collection
  stops after the screen leaves but survives rotation. Screens holding input (editor, share,
  launch) start eagerly.
- Dependencies come through the constructor (`@Inject`).

## Failures

- Only storage failures are recovered from: the data layer wraps what Room or SQLite throws in
  `StorageException`. Anything else is a programmer error and is not caught. Every recovered
  failure goes to `ErrorReporter` (the system log).
- A failed read becomes the screen's `Failed` state; `Retry` reads again (`Retryable`).
- A failed write (pin, delete, save) leaves the data as it was and shows a failure until
  `DismissFailure`. Cancellation is always rethrown.
- The editor's phases (loading, editing, saving, deleting, finished) are one pure function,
  `EditorPhase.on`, tested directly. On Android the editor session survives process death
  (`EditorTarget.Resume`).
- The reminder and widget syncs run for the process lifetime, report failures and retry.

## Platforms

Android collects `uiState` with `collectAsStateWithLifecycle()`, and `ViewModelStore` owns the
ViewModel. On iOS, KMP-ObservableViewModel and `@NativeCoroutinesState` make `uiState` a plain
property SwiftUI observes, and `@StateViewModel` ties the ViewModel to the view
([ios-interop.md](ios-interop.md)). Each platform turns `DayCount` and `CalendarDate` into text
with its own plural rules and date formatters.
