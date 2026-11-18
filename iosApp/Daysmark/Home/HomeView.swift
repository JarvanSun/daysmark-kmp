import KMPObservableViewModelSwiftUI
import Shared
import SwiftUI

/// Home renders `HomeUiState` and sends `HomeAction`s; it computes nothing itself.
struct HomeView: View {
    @StateViewModel private var viewModel: HomeViewModel
    @State private var pendingDeletion: EventRow?
    let router: AppRouter

    /// The ViewModel is built only the first time this screen appears: `StateViewModel` keeps it
    /// afterwards, so a parent re-rendering never creates (and leaks) another one.
    init(viewModel: @autoclosure @escaping () -> HomeViewModel, router: AppRouter) {
        _viewModel = StateViewModel(wrappedValue: viewModel())
        self.router = router
    }

    var body: some View {
        Group {
            if let content = viewModel.uiState as? HomeUiStateContent {
                list(content)
                    .failureAlert(
                        content.failure == .pinning ? "failure.pin.title" : "failure.delete.title",
                        isPresented: content.failure != nil
                    ) { viewModel.onAction(action: HomeActionDismissFailure.shared) }
            } else if viewModel.uiState is HomeUiStateFailed {
                LoadFailedView { viewModel.onAction(action: HomeActionRetry.shared) }
            } else {
                Color.clear
            }
        }
        .background(.daysmark(.canvas))
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("home.toolbar.settings", systemImage: "gearshape") { router.openSettings() }
            }
            ToolbarItem(placement: .primaryAction) {
                Button(action: addEvent) {
                    Label("home.toolbar.add", systemImage: "plus")
                }
            }
        }
    }

    private func list(_ content: HomeUiStateContent) -> some View {
        List {
            if content.showsOverview {
                overview(content)
            }
            ForEach(content.rows, id: \.id.value) { row in
                Button {
                    router.openDetail(row.id)
                } label: {
                    EventRowView(row: row)
                }
                .buttonStyle(.plain)
                .swipeActions(edge: .leading) {
                    Button {
                        viewModel.onAction(action: HomeActionTogglePin(id: row.id))
                    } label: {
                        Label(
                            row.isPinned ? "home.row.unpin" : "home.row.pin",
                            systemImage: row.isPinned ? "pin.slash" : "pin")
                    }
                    .tint(.daysmark(.tint))
                }
                .swipeActions(edge: .trailing) {
                    Button("home.row.delete", systemImage: "trash", role: .destructive) {
                        pendingDeletion = row
                    }
                }
                .listRowBackground(Color.clear)
                .listRowSeparatorTint(.daysmark(.separator))
            }
            if let placeholder = content.placeholder {
                HomeEmptyView(
                    placeholder: placeholder,
                    onAdd: addEvent,
                    onResetFilters: { viewModel.onAction(action: HomeActionResetFilters.shared) }
                )
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        // An alert, not a confirmation dialog: on iOS 26 a dialog anchors to the view it's attached
        // to, and one on the swiped row is dismissed as the row closes.
        .alert(
            Text("home.delete.title \(pendingDeletion?.title ?? "")"),
            isPresented: isConfirmingDeletion,
            presenting: pendingDeletion
        ) { row in
            Button("home.delete.confirm", role: .destructive) {
                viewModel.onAction(action: HomeActionDelete(id: row.id))
            }
            Button("home.delete.cancel", role: .cancel) {}
        } message: { _ in
            Text("home.delete.message")
        }
    }

    @ViewBuilder
    private func overview(_ content: HomeUiStateContent) -> some View {
        Group {
            if let focus = content.focus {
                FocusCard(row: focus)
                    .padding(.top, Dimensions.spaceSmall)
                    .padding(.bottom, Dimensions.spaceXLarge)
            }
            LandscapeView(counts: content.landscape)
                .padding(.bottom, Dimensions.spaceXLarge)
            FilterBar(filters: content.filters) { category in
                viewModel.onAction(action: HomeActionSelectCategory(category: category))
            }
            .padding(.bottom, Dimensions.spaceMedium)
        }
        .listRowBackground(Color.clear)
        .listRowInsets(EdgeInsets(top: 0, leading: Dimensions.pageMargin, bottom: 0, trailing: Dimensions.pageMargin))
        .listRowSeparator(.hidden)
    }

    private var isConfirmingDeletion: Binding<Bool> {
        Binding(get: { pendingDeletion != nil }, set: { if !$0 { pendingDeletion = nil } })
    }

    /// A new event starts in the category Home is filtered to.
    private func addEvent() {
        let selected = (viewModel.uiState as? HomeUiStateContent)?.filters.first { $0.isSelected }?.category
        router.newEvent(in: selected ?? .important)
    }
}
