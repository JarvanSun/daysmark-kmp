# Architecture

Kotlin Multiplatform with **native UI on each platform**. Business rules, storage and screen
state are written once in Kotlin; SwiftUI and Jetpack Compose draw them. The UI stays native
because the app relies on each platform's own controls: swipe actions, the iPad split view,
system dialogs, widgets.

The layering is Clean Architecture (dependencies point inward, toward the domain) with
unidirectional data flow in the presentation layer ([presentation.md](presentation.md)).

## Modules

| Module | Owns | Depends on |
|---|---|---|
| `core:domain` | Models, rules (`DayCountCalculator`), repository and platform interfaces | Kotlin, kotlinx-coroutines, kotlinx-datetime only |
| `core:data` | Room database, DAOs, repository implementations | `core:domain` |
| `feature:events` | ViewModels, `UiState`, `Action` | `core:domain` |
| `shared` | The Metro graph, platform entry points, the iOS framework | all of the above |
| `androidApp` | Compose screens, resources, `Application` | `shared` |
| `iosApp` | SwiftUI views, widget extension, String Catalog | the `Shared` framework |
| `core:testing` | Fakes for tests | `core:domain` |
| `tests:architecture` | Konsist rules over the sources | — |

Platform capabilities are domain interfaces with one implementation per platform:
`ReminderScheduler` (UserNotifications / AlarmManager) and `WidgetPublisher` (App Group +
WidgetKit / file + Glance).

## Dependency rules

| Rule | Enforced by |
|---|---|
| A feature never sees `core:data` | No Gradle dependency; `LayerRulesTest` |
| `core:domain` imports no Android, Room, Metro or iOS API | `LayerRulesTest` |
| Repository interfaces live in the domain; implementations are `internal` to `core:data` | `LayerRulesTest` |
| ViewModels expose exactly `uiState` and `onAction` | `PresentationRulesTest` |
| Every public declaration is deliberate | `explicitApi()` |
| Production code never uses test fakes | `LayerRulesTest` |

## Where code goes

- **A date or event rule**: a pure function in `core:domain`, with "now" passed in (`WallClock`).
- **Stored data**: a repository interface in the domain, implemented in `core:data`.
- **A use case** only when a flow crosses several repositories or services with rules of its own
  (reminder and widget sync). A class that would only forward to one repository is not created.
- **Text the user reads**: built by the platform from structured values (`DayCount`,
  `CalendarDate`). Shared code never builds user-facing strings.
- **Naming**: one screen is `<Screen>ViewModel`, `<Screen>UiState`, `<Screen>Action`;
  implementations are named by technology (`RoomEventRepository`), never `Impl`.

## Dependency injection

[Metro](https://github.com/ZacSweers/metro) builds the graph at compile time: a missing or
duplicate binding is a build error, not a runtime crash. That is why it was chosen over Koin.

- Platforms see a plain `AppGraph` interface (ViewModels, factories, `AppServices`); Metro
  implements it with an `internal` graph, so Room and Metro types never reach Swift.
- What a platform must supply is a graph factory parameter: database location, clock, error
  reporter, `ReminderScheduler`, `WidgetPublisher`. The compiler makes every platform pass all.
- `core:data` binds its `internal` implementations in its own `@BindingContainer`. The domain has
  no DI annotations. Only the database is a singleton; ViewModels are unscoped.
- Tests don't use the graph: they call constructors with fakes.

## Persistence

Room (KMP) with the bundled SQLite driver: one schema and one set of DAOs on Android, iOS and the
JVM tests.

| Table | Holds |
|---|---|
| `events` | Year, month, day, and enums by stable `storageKey` (never name or ordinal) |
| `reminders` | Days before and time of day; foreign key with `CASCADE` |
| `pinned_event` | At most one row: every write uses slot 0, and a foreign key points at an event |
| `onboarding` | One row once onboarding is done |

- Multi-table writes (save with reminders, delete with pin, onboarding) are one transaction.
- Home reads events and the pin as one snapshot, so they always agree.
- The schema is exported to `core/data/schemas/` and committed. From the first release, every
  change raises the version with a migration and a test.
