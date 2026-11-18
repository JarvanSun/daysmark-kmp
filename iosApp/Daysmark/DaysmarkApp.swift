import KMPObservableViewModelSwiftUI
import Shared
import SwiftUI

@main
struct DaysmarkApp: App {
    @UIApplicationDelegateAdaptor private var delegate: AppDelegate
    /// The one Kotlin dependency graph; ViewModels come from it.
    private let graph = IosAppGraphKt.createAppGraph(
        reminderScheduler: SystemReminderScheduler(), widgetPublisher: WidgetTimelinePublisher(),
        errorReporter: LogErrorReporter())

    init() {
        graph.services.start()
    }

    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            LaunchView(graph: graph)
                // The phone may have slept through a midnight, or the time zone changed: plan
                // reminders and the widget again from the current clock.
                .onReceive(NotificationCenter.default.publisher(for: UIApplication.significantTimeChangeNotification)) {
                    _ in
                    graph.services.onClockChanged()
                }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { graph.services.onClockChanged() }
        }
    }
}

/// Onboarding on first run, Home ever after. Waits (on the page colour) until the flag is read.
struct LaunchView: View {
    let graph: AppGraph
    @StateViewModel private var launch: LaunchViewModel
    /// Owned here, not by Home, so a link or notification that arrives while the launch state is
    /// still loading (a cold launch) is kept and shown once Home appears.
    @State private var router = AppRouter()
    /// Only finishing onboarding fades into Home; an ordinary launch shows Home at once.
    @State private var wasOnboarding = false
    @EnvironmentObject private var delegate: AppDelegate
    private static let fade = 0.18

    init(graph: AppGraph) {
        self.graph = graph
        _launch = StateViewModel(wrappedValue: graph.launchViewModel)
    }

    var body: some View {
        ZStack {
            switch launch.uiState {
            case .home:
                RootView(graph: graph, router: router)
                    .transition(.opacity.animation(.easeOut(duration: Self.fade).delay(Self.fade)))
            case .onboarding:
                NavigationStack { OnboardingView(viewModel: graph.onboardingViewModel) }
                    .transition(.opacity.animation(.easeIn(duration: Self.fade)))
                    .onAppear { wasOnboarding = true }
            case .failed:
                LoadFailedView { launch.onAction(action: LaunchActionRetry.shared) }
            default:
                Color.daysmark(.canvas).ignoresSafeArea()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        // Between the two screens the page colour shows.
        .background(.daysmark(.canvas))
        // Onboarding fades out completely before Home fades in, so they never draw over each other.
        .animation(wasOnboarding ? .linear(duration: Self.fade * 2) : nil, value: launch.uiState)
        // A tapped reminder or widget opens its event.
        .onAppear { delegate.openEvent = { id in router.show(EventId(value: id)) } }
        .onOpenURL { url in
            switch DeepLink(url) {
            case let .event(id): router.show(EventId(value: id))
            case .newEvent: router.newEvent(in: .important)
            case nil: break
            }
        }
    }
}

/// The navigation stack and the sheet; every screen gets its ViewModel from the graph here.
struct RootView: View {
    let graph: AppGraph
    @Bindable var router: AppRouter

    var body: some View {
        NavigationStack(path: $router.path) {
            HomeView(viewModel: graph.homeViewModel, router: router)
                .navigationDestination(for: AppRouter.Route.self) { route in
                    destination(route)
                }
        }
        .sheet(item: $router.sheet) { sheet in
            switch sheet {
            case let .newEvent(category):
                EditorView(viewModel: graph.editorViewModelFactory.create(target: EditorTargetNew(category: category)))
            case let .edit(eventID):
                EditorView(
                    viewModel: graph.editorViewModelFactory.create(
                        target: EditorTargetExisting(id: EventId(value: eventID))))
            case let .share(eventID):
                ShareView(viewModel: graph.shareViewModelFactory.create(eventId: EventId(value: eventID)))
            }
        }
    }

    @ViewBuilder
    private func destination(_ route: AppRouter.Route) -> some View {
        switch route {
        case let .detail(eventID):
            DetailView(
                viewModel: graph.detailViewModelFactory.create(eventId: EventId(value: eventID)), router: router
            )
            // A link that replaces this event with another must build a new screen and ViewModel;
            // otherwise SwiftUI keeps the old one at the same place in the stack.
            .id(eventID)
        case .settings:
            SettingsView()
        }
    }
}
