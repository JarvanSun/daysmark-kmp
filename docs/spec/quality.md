# Quality

`scripts/verify.sh` is the single gate, locally and in CI. `kotlin` runs the design-token check,
the Room schema check and `./gradlew check`; `ios` runs swift-format, the Xcode build and the
Swift tests.

## Gates

| Gate | Stops |
|---|---|
| `explicitApi()`, warnings as errors | Accidental public API, leftover warnings |
| ktfmt (Spotless), detekt | Formatting, complexity, `lateinit` |
| Konsist (`tests/architecture`) | Layer and ViewModel rule violations, `!!`, fakes in production |
| Module graph, Metro | A feature reaching the data layer; missing bindings, at compile time |
| `checkFrameworkApi` | Unreviewed changes to the iOS framework's API |
| Shared fixtures (`resources/fixtures`) | Kotlin and Swift disagreeing on the widget format: both read the same valid file and reject the same invalid ones |
| Swift mapping tests | A Kotlin case Swift doesn't handle (`SwiftTestExamples` has an exhaustive `when`) |
| swift-format `--strict`, Xcode build | Swift formatting; Swift no longer compiling against Kotlin |

Every Kotlin module applies a convention plugin from `build-logic`, so none can opt out.

## Tests

| Layer | How | Example |
|---|---|---|
| Domain | `commonTest` on the JVM, Android host and iOS simulator | `DayCountCalculatorTest` |
| Repositories | A real in-memory Room database, including a mid-transaction failure that must roll back | `TransactionRollbackTest` |
| ViewModels | Fakes with injectable failures, owned and cleared like on a device | `HomeViewModelTest`, `LifecycleTest` |
| Editor state machine | The pure transition function | `EditorMachineTest` |
| Swift | Swift Testing, app-hosted | `SharedMappingTests` |

- **Fakes, not mocks**: hand-written fakes in `core:testing`, no mocking library.
- **No real time**: `FixedWallClock` and `VirtualClock`; midnight and retries run in milliseconds.
- **One behaviour per test**, named as a sentence.
- A new rule or check is shown to fail on a deliberate violation before it is committed.
- Platform UI is checked by hand (English and Chinese, light and dark); there are no screenshot
  tests yet.

## Conventions

- Visibility starts at `internal`; immutable data across boundaries.
- Invalid values can't be constructed (`CalendarDate`); an expected absence is `null`.
- Comments explain why; a public type has a KDoc.
- Swift 6, main actor by default. Views render state and never compute, sort or filter.
- No absolute machine path in the repository.
