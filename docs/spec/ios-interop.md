# iOS interop

The iOS app is a SwiftUI app that links one static Kotlin framework, `Shared`, built by Gradle
during the Xcode build.

## The framework

- It exports only `feature:events` and `core:domain`. `core:data`, Room and Metro are linked in
  but invisible to Swift.
- `checkFrameworkApi` compares the exported types with the reviewed list
  `shared/api/framework-types.txt`; `updateFrameworkApi` rewrites it.
- Swift creates the graph once in `DaysmarkApp`, passing its own `ReminderScheduler`,
  `WidgetPublisher` and `ErrorReporter`.
- Bridging: [KMP-NativeCoroutines](https://github.com/rickclephas/KMP-NativeCoroutines)
  (compiler plugin only) and [KMP-ObservableViewModel](https://github.com/rickclephas/KMP-ObservableViewModel).
  One retroactive conformance makes every ViewModel observable; there are no per-type wrappers.

## Kotlin in Swift

| Kotlin | Swift |
|---|---|
| `@NativeCoroutinesState val uiState` | `viewModel.uiState`, a plain observed property |
| `sealed interface HomeUiState` / `Content` | `HomeUiStateContent`, matched with `as?` |
| `HomeAction.TogglePin(id)`, an `object` case | `HomeActionTogglePin(id:)`, `EditorActionSave.shared` |
| `suspend fun` in an interface | `async throws` |
| `Int` | `Int32`; converted at the view edge |
| `value class` | `Any`, so exported types use data classes |
| Enum entry `NO_MATCHES` | `.noMatches`; exported enums must be UPPER_SNAKE_CASE (Konsist) |
| Names starting with `new`, `alloc`, `copy`, `init` | Renamed by Objective-C; avoided |
| C keywords (`DELETE`) | Renamed to `delete_`; avoided |

Swift classes Kotlin calls synchronously from a background thread (`LogErrorReporter`) are
`nonisolated`, because the app's default isolation is the main actor.

## The Xcode project

- `iosApp/project.yml` is the source of truth; `xcodegen` generates `DaysmarkKMP.xcodeproj`,
  which is not committed.
- A pre-build phase runs `scripts/build-shared-framework.sh`
  (`embedAndSignAppleFrameworkForXcode`). Xcode reads no shell profile, so the script finds a JDK
  itself (`scripts/find-jdk.sh`).

| Folder | Target | Holds |
|---|---|---|
| `iosApp/Daysmark` | App | One folder per screen, `Components`, `Formatting`, platform services |
| `iosApp/DaysmarkWidget` | Widget extension | The widget and the timeline's Codable mirror |
| `iosApp/Common` | Both | Design tokens, artwork, phrases, `AppGroup`, `DeepLink` |

The widget extension doesn't link Kotlin: it stays small and only shows what the app computed.
Widget and notification taps arrive as `daysmark-kmp://event/<id>` or `daysmark-kmp://new`.
