# Product

Daysmark tracks the dates that matter: countdowns that keep counting up once the day has passed,
birthdays and anniversaries. It is not a task tool: no "done", "due" or "overdue". No account, no
ads, no tracking; data stays on the device.

## Events

- An event has a title, a date, an optional end date and time, a repeat rule (never, monthly,
  yearly), a category (Important Date, Birthday, Anniversary), an icon and reminders.
- **Dates are what the user saw**: year, month and day (Gregorian), never an instant or a time
  zone.
- **Strict dates**: 30 February does not exist, a monthly event on the 31st skips shorter months,
  and a yearly event on 29 February happens only in leap years.
- A repeating event shows its next occurrence. A range counts to its start, then to its end. A
  past one-time event stays, counting up, until it is deleted.

## Relative time

Every surface uses one complete, localized phrase per `DayCount`: `Future(12)` is "in 12 days",
`Past(12)` is "12 days ago", `Today` is "Today". Never "0 days", "12 days left" or a bare number,
and future and past are never told apart by colour alone.

## Screens

- **Home**: the focused event (pinned, or the nearest), the landscape illustration, category
  filters and the list: today and the future first, nearest first, then the past.
- **Detail**: a large count that cycles through days, weeks, hours and minutes, a ticking line,
  share, pin and edit.
- **Editor**: one sheet that creates and edits, with a live preview row. Delete asks first.
- **Share**: a rendered card and the system share sheet.
- **Settings**: notifications, adding the widget, version.
- **Onboarding**: welcome, the user's birthday and ready-made events. Every step can be skipped;
  notifications are requested only for the birthday, which comes with reminders. Finishing saves
  the events and the "done" mark in one transaction, with fixed ids so it can't duplicate.

## Platforms

| | iOS | Android |
|---|---|---|
| UI | SwiftUI, iPhone and iPad | Jetpack Compose |
| Widget | WidgetKit | Glance |
| Reminders | UserNotifications | AlarmManager, rescheduled after reboot |
| Languages | English, Simplified Chinese | English, Simplified Chinese |

Shared Kotlin owns every rule; each platform writes only the system integration.

**Widget.** The widget never opens the database. The app computes a timeline (the focused event
now and at each of the next 30 midnights, with Home's rules), and `WidgetSync` republishes it as
versioned JSON whenever events, the pin or the day change. iOS stores it in the App Group and the
extension decodes it with a Codable mirror; Android stores it in the app's files and redraws at
the next entry with an alarm. A tap opens the event, or a new one when there are none.

Not built: sync, custom categories, lunar calendars, Siri and Live Activities.
