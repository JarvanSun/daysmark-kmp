import KMPObservableViewModelSwiftUI
import Shared
import SwiftUI
import UserNotifications

/// One sheet creates and edits an event. It renders `EditorUiState` and sends `EditorAction`s.
struct EditorView: View {
    @Environment(\.dismiss) private var dismiss
    @StateViewModel private var viewModel: EditorViewModel
    @FocusState private var isTitleFocused: Bool
    @State private var isConfirmingDelete = false

    /// The ViewModel is built only the first time this screen appears: `StateViewModel` keeps it
    /// afterwards, so a parent re-rendering never creates (and leaks) another one.
    init(viewModel: @autoclosure @escaping () -> EditorViewModel) {
        _viewModel = StateViewModel(wrappedValue: viewModel())
    }

    var body: some View {
        NavigationStack {
            Group {
                if let state = viewModel.uiState as? EditorUiStateEditing {
                    form(state)
                        .interactiveDismissDisabled(state.hasChanges)
                        .navigationTitle(state.isNew ? "editor.title.new" : "editor.title.edit")
                        .toolbar { toolbar(state) }
                } else if viewModel.uiState is EditorUiStateFailed {
                    LoadFailedView { send(EditorActionRetry.shared) }
                } else {
                    Color.clear
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .background(.daysmark(.canvas))
        }
        .tint(.daysmark(.tint))
        // `initial`: an editor opened on an event that is already gone closes at once.
        .onChange(of: viewModel.uiState is EditorUiStateFinished, initial: true) { _, finished in
            if finished { dismiss() }
        }
    }

    private func form(_ state: EditorUiStateEditing) -> some View {
        let draft = state.draft
        return List {
            Group {
                Section {
                    EditorPreviewRow(title: draft.title, preview: state.preview)
                        .listRowSeparator(.hidden)
                    TextField(
                        "editor.titleField.placeholder",
                        text: Binding(get: { draft.title }, set: { send(EditorActionSetTitle(title: $0)) }),
                        axis: .vertical
                    )
                    .font(DaysmarkFont.heroTitle.font)
                    .submitLabel(.done)
                    .focused($isTitleFocused)
                }
                EditorDatesSection(draft: draft, send: send)
                Section("editor.category.title") {
                    Picker("editor.category.title", selection: categoryBinding(draft)) {
                        ForEach(EventCategory.entries, id: \.self) { category in
                            Text(category.displayName).tag(category)
                        }
                    }
                    .pickerStyle(.segmented)
                    .labelsHidden()
                }
                EditorRulesSection(state: state, send: send, onAddReminder: addReminder)
                EditorIconSection(selection: draft.icon) { send(EditorActionSetIcon(icon: $0)) }
                if !state.isNew {
                    Section {
                        Button("editor.delete.button", role: .destructive) { isConfirmingDelete = true }
                            .disabled(state.isBusy)
                            .frame(maxWidth: .infinity)
                    }
                }
            }
            // The original's look: rows on the page's own colour, not grouped cards.
            .listRowBackground(Color.clear)
            .listSectionSeparator(.hidden, edges: .top)
        }
        .listStyle(.plain)
        // Its date pickers show Gregorian dates whatever the system calendar is.
        .environment(\.calendar, Gregorian.calendar)
        .scrollContentBackground(.hidden)
        .scrollDismissesKeyboard(.immediately)
        .task { if state.isNew { isTitleFocused = true } }
        .failureAlert(
            state.failure == .deleting ? "failure.delete.title" : "failure.save.title",
            isPresented: state.failure != nil
        ) { send(EditorActionDismissFailure.shared) }
        .alert("editor.delete.title", isPresented: $isConfirmingDelete) {
            Button("home.delete.confirm", role: .destructive) { send(EditorActionDelete.shared) }
            Button("home.delete.cancel", role: .cancel) {}
        } message: {
            Text("home.delete.message")
        }
    }

    @ToolbarContentBuilder
    private func toolbar(_ state: EditorUiStateEditing) -> some ToolbarContent {
        ToolbarItem(placement: .cancellationAction) {
            Button("editor.toolbar.cancel", systemImage: "xmark") { dismiss() }
        }
        ToolbarItem(placement: .confirmationAction) {
            Button("editor.toolbar.save", systemImage: "checkmark") { send(EditorActionSave.shared) }
                .buttonStyle(.glassProminent)
                .disabled(!state.canSave)
        }
    }

    private func categoryBinding(_ draft: EventDraft) -> Binding<EventCategory> {
        Binding(get: { draft.category }, set: { send(EditorActionSetCategory(category: $0)) })
    }

    /// Permission is asked when the user first adds a reminder, never at launch.
    private func addReminder() {
        send(EditorActionAddReminder.shared)
        Task {
            let center = UNUserNotificationCenter.current()
            if await center.notificationSettings().authorizationStatus == .notDetermined {
                _ = try? await center.requestAuthorization(options: [.alert, .sound, .badge])
            }
        }
    }

    private func send(_ action: EditorAction) {
        viewModel.onAction(action: action)
    }
}
