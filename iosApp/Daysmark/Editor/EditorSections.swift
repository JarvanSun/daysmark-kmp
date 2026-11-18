import Shared
import SwiftUI

/// Start date, an optional time and an optional end date.
struct EditorDatesSection: View {
    let draft: EventDraft
    let send: (EditorAction) -> Void

    var body: some View {
        Section {
            DatePicker(
                "editor.date.start",
                selection: Binding(get: { draft.date.date }, set: { send(EditorActionSetDate(date: .from($0))) }),
                displayedComponents: .date)
            Toggle(
                "editor.date.addTime",
                isOn: Binding(
                    get: { draft.time != nil },
                    set: { send(EditorActionSetTime(time: $0 ? TimeOfDay(hour: 19, minute: 0) : nil)) }))
            if let time = draft.time {
                DatePicker(
                    "editor.date.time",
                    selection: Binding(get: { time.date }, set: { send(EditorActionSetTime(time: .from($0))) }),
                    displayedComponents: .hourAndMinute)
            }
            Toggle(
                "editor.date.addEnd",
                isOn: Binding(
                    get: { draft.endDate != nil },
                    set: { send(EditorActionSetEndDate(endDate: $0 ? draft.date.adding(days: 1) : nil)) }))
            if let end = draft.endDate {
                DatePicker(
                    "editor.date.end",
                    selection: Binding(get: { end.date }, set: { send(EditorActionSetEndDate(endDate: .from($0))) }),
                    in: draft.date.date...,
                    displayedComponents: .date)
            }
        }
    }
}

/// Reminders, then the repeat rule.
struct EditorRulesSection: View {
    let state: EditorUiStateEditing
    let send: (EditorAction) -> Void
    let onAddReminder: () -> Void

    var body: some View {
        Section("editor.rules.title") {
            ForEach(Array(state.draft.reminders.enumerated()), id: \.offset) { index, reminder in
                ReminderRow(reminder: reminder, choices: state.reminderChoices.map { Int(truncating: $0) }) {
                    send(EditorActionUpdateReminder(index: Int32(index), reminder: $0))
                }
                .swipeActions {
                    Button("home.row.delete", systemImage: "trash", role: .destructive) {
                        send(EditorActionRemoveReminder(index: Int32(index)))
                    }
                }
            }
            Button("editor.reminder.add", systemImage: "bell.badge", action: onAddReminder)
            Picker(
                "editor.repeat.title",
                selection: Binding(get: { state.draft.repeat }, set: { send(EditorActionSetRepeat(repeat: $0)) })
            ) {
                ForEach(RepeatRule.entries, id: \.self) { rule in
                    Text(rule.displayName).tag(rule)
                }
            }
        }
    }
}

/// "Remind me: 1 day before · 9:00".
private struct ReminderRow: View {
    let reminder: Reminder
    let choices: [Int]
    let onChange: (Reminder) -> Void

    var body: some View {
        HStack {
            Picker(
                "editor.reminder.offset",
                selection: Binding(
                    get: { Int(reminder.daysBefore) },
                    set: { onChange(Reminder(daysBefore: Int32($0), time: reminder.time)) })
            ) {
                ForEach(choices, id: \.self) { days in
                    Text(ReminderRow.label(days)).tag(days)
                }
            }
            DatePicker(
                "editor.reminder.time",
                selection: Binding(
                    get: { reminder.time.date },
                    set: { onChange(Reminder(daysBefore: reminder.daysBefore, time: .from($0))) }),
                displayedComponents: .hourAndMinute
            )
            .labelsHidden()
        }
    }

    static func label(_ days: Int) -> String {
        days == 0 ? String(localized: "reminder.onTheDay") : String(localized: "reminder.daysBefore \(days)")
    }
}

/// The illustrated icons; the selected one sits on a soft tint.
struct EditorIconSection: View {
    let selection: EventIcon
    let onSelect: (EventIcon) -> Void

    var body: some View {
        Section("editor.appearance.title") {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 52), spacing: Dimensions.spaceSmall)]) {
                ForEach(EventIcon.entries, id: \.self) { icon in
                    Button {
                        onSelect(icon)
                    } label: {
                        EventIconView(icon: icon, size: 36)
                            .frame(width: 52, height: 52)
                            .background(
                                icon == selection ? DaysmarkColor.tintSoft.color : .clear,
                                in: .rect(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(icon.label)
                    .accessibilityAddTraits(icon == selection ? .isSelected : [])
                }
            }
            .padding(.vertical, Dimensions.spaceSmall)
        }
    }
}

extension RepeatRule {
    var displayName: String {
        switch self {
        case .monthly: String(localized: "repeat.monthly")
        case .yearly: String(localized: "repeat.yearly")
        default: String(localized: "repeat.never")
        }
    }
}

extension EventIcon {
    var label: String {
        switch self {
        case .star: String(localized: "eventIcon.star")
        case .gift: String(localized: "eventIcon.gift")
        case .heart: String(localized: "eventIcon.heart")
        case .airplane: String(localized: "eventIcon.airplane")
        case .graduationCap: String(localized: "eventIcon.graduationCap")
        case .birthdayCake: String(localized: "eventIcon.birthdayCake")
        case .briefcase: String(localized: "eventIcon.briefcase")
        case .house: String(localized: "eventIcon.house")
        case .running: String(localized: "eventIcon.running")
        case .book: String(localized: "eventIcon.book")
        default: String(localized: "eventIcon.calendar")
        }
    }
}
