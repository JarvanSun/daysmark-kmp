import KMPObservableViewModelSwiftUI
import Shared
import SwiftUI
import UserNotifications

/// The first-run introduction: welcome, the user's birthday, then ready-made events. Renders
/// `OnboardingUiState` and sends `OnboardingAction`s; the flow's rules live in Kotlin.
struct OnboardingView: View {
    @StateViewModel private var viewModel: OnboardingViewModel

    /// The ViewModel is built only the first time this screen appears: `StateViewModel` keeps it
    /// afterwards, so a parent re-rendering never creates (and leaks) another one.
    init(viewModel: @autoclosure @escaping () -> OnboardingViewModel) {
        _viewModel = StateViewModel(wrappedValue: viewModel())
    }

    private var state: OnboardingUiState { viewModel.uiState }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Dimensions.spaceXLarge) {
                header
                sentences
                LandscapeView(counts: state.landscape)
                stepContent
            }
            .padding(.horizontal, Dimensions.pageMargin)
            .padding(.top, Dimensions.spaceXLarge)
            .padding(.bottom, Dimensions.spaceMedium)
            .frame(maxWidth: Dimensions.readableWidth)
            .frame(maxWidth: .infinity)
        }
        .scrollBounceBehavior(.basedOnSize)
        .background(.daysmark(.canvas))
        .safeAreaBar(edge: .bottom) { primaryButton }
        .overlay {
            if state.celebratesBirthday { ConfettiView().allowsHitTesting(false) }
        }
        .toolbar { toolbar }
        .navigationBarTitleDisplayMode(.inline)
        .sensoryFeedback(.impact(weight: .light), trigger: state.step)
        .alert("onboarding.failure.title", isPresented: saveFailed) {
            Button("onboarding.failure.retry") { send(OnboardingActionRetrySave.shared) }
            Button("onboarding.failure.cancel", role: .cancel) {}
        } message: {
            Text("onboarding.failure.message")
        }
    }

    // MARK: - Content

    private var header: some View {
        VStack(alignment: .leading, spacing: Dimensions.spaceMedium) {
            Text(state.step.title)
                .textStyle(.textPrimary, .pageTitle)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Text(state.step.message)
                .textStyle(.textSecondary, .pageSubtitle)
                .fixedSize(horizontal: false, vertical: true)
        }
        // A new step replaces the text at once, with no cross-fade.
        .id(state.step)
        .transition(.identity)
    }

    @ViewBuilder
    private var sentences: some View {
        if state.headline != nil || state.detail != nil {
            VStack(alignment: .leading, spacing: Dimensions.spaceMedium) {
                if let headline = state.headline {
                    StarterSentence(count: headline, titleFont: .heroTitle, phraseFont: .heroPhrase)
                }
                if let detail = state.detail {
                    StarterSentence(count: detail, titleFont: .eventTitle, phraseFont: .rowPhrase)
                }
            }
        }
    }

    @ViewBuilder
    private var stepContent: some View {
        switch state.step {
        case .birthday:
            DatePicker(
                "onboarding.birthday.picker",
                selection: Binding(
                    get: { state.birthday.date },
                    set: { send(OnboardingActionSetBirthday(date: .from($0))) }),
                in: ...Date.now,
                displayedComponents: .date
            )
            .datePickerStyle(.wheel)
            .environment(\.calendar, Gregorian.calendar)
            .labelsHidden()
            .frame(maxWidth: .infinity)
        case .starters:
            VStack(spacing: 0) {
                ForEach(Array(state.choices.enumerated()), id: \.element.starter) { index, choice in
                    if index > 0 { Divider().padding(.leading, Dimensions.selectionMarkInset) }
                    StarterChoiceRow(choice: choice) {
                        send(OnboardingActionToggle(starter: choice.starter))
                    }
                }
            }
        default:
            EmptyView()
        }
    }

    private var primaryButton: some View {
        Button {
            if state.primaryAction == .next {
                send(OnboardingActionContinue.shared)
            } else {
                finish()
            }
        } label: {
            Text(state.primaryAction.title)
                .font(DaysmarkFont.buttonLabel.font)
                .frame(maxWidth: .infinity, minHeight: Dimensions.primaryButtonHeight)
        }
        .buttonStyle(.glassProminent)
        .tint(.daysmark(.tint))
        .disabled(!state.canContinue)
        .padding(.horizontal, Dimensions.pageMargin)
        .padding(.top, Dimensions.spaceMedium)
        .padding(.bottom, Dimensions.spaceSmall)
    }

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        if state.canGoBack {
            ToolbarItem(placement: .topBarLeading) {
                Button("onboarding.toolbar.back", systemImage: "chevron.backward") {
                    send(OnboardingActionBack.shared)
                }
            }
        }
        switch state.step {
        case .welcome:
            ToolbarItem(placement: .topBarTrailing) {
                Button("onboarding.toolbar.skip") { send(OnboardingActionStartEmpty.shared) }
            }
        case .birthday:
            ToolbarItem(placement: .topBarTrailing) {
                Button("onboarding.toolbar.skip") { send(OnboardingActionSkipBirthday.shared) }
                    .accessibilityLabel(Text("onboarding.toolbar.skipBirthday"))
            }
        default:
            ToolbarItem(placement: .topBarTrailing) { EmptyView() }
        }
    }

    // MARK: - Actions

    private var saveFailed: Binding<Bool> {
        Binding(
            get: { state.saveFailed },
            set: { if !$0 { send(OnboardingActionDismissSaveFailure.shared) } })
    }

    private func finish() {
        let asksForNotifications = state.finishAsksForNotifications
        let titles = Dictionary(uniqueKeysWithValues: StarterEvent.entries.map { ($0, $0.title) })
        send(OnboardingActionFinish(titles: titles))
        // The birthday comes with reminders; ask now so they can arrive (only if never asked).
        if asksForNotifications {
            Task {
                let center = UNUserNotificationCenter.current()
                if await center.notificationSettings().authorizationStatus == .notDetermined {
                    _ = try? await center.requestAuthorization(options: [.alert, .sound, .badge])
                }
            }
        }
    }

    private func send(_ action: OnboardingAction) {
        viewModel.onAction(action: action)
    }
}

/// "New Year" over "in 44 days".
private struct StarterSentence: View {
    let count: StarterCount
    let titleFont: DaysmarkFont
    let phraseFont: DaysmarkFont

    var body: some View {
        VStack(alignment: .leading, spacing: Dimensions.spaceCompact) {
            Text(verbatim: count.starter.title)
                .textStyle(.textPrimary, titleFont)
            Text(verbatim: count.isPending ? "—" : count.dayCount.phrase)
                .textStyle(count.isPending ? .textTertiary : count.dayCount.tint, phraseFont)
                .monospacedDigit()
                .contentTransition(.numericText(countsDown: count.dayCount.countsDown))
                .animation(.default, value: count.dayCount.phrase)
        }
        .accessibilityElement(children: .combine)
    }
}

/// A ready-made event the user keeps or leaves out.
private struct StarterChoiceRow: View {
    let choice: StarterChoice
    let onToggle: () -> Void

    var body: some View {
        Button(action: onToggle) {
            HStack(alignment: .firstTextBaseline, spacing: Dimensions.spaceMedium) {
                Image(systemName: choice.isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundStyle(.daysmark(choice.isSelected ? .tint : .textTertiary))
                    .frame(width: Dimensions.selectionMarkWidth)
                VStack(alignment: .leading, spacing: Dimensions.rowLineSpacing) {
                    Text(verbatim: choice.starter.title)
                        .textStyle(.textPrimary, .eventTitle)
                    Text(verbatim: choice.dayCount.phrase)
                        .textStyle(choice.dayCount.tint, .rowPhrase)
                        .monospacedDigit()
                    if choice.starter == .birthday {
                        Text("onboarding.starter.birthday.reminders")
                            .textStyle(.textSecondary, .rowSecondary)
                    }
                }
                .opacity(choice.isSelected ? 1 : 0.55)
                Spacer(minLength: 0)
            }
            .padding(.vertical, Dimensions.spaceMedium)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(choice.isSelected ? .isSelected : [])
        .sensoryFeedback(.selection, trigger: choice.isSelected)
    }
}

extension OnboardingStep {
    var title: LocalizedStringKey {
        switch self {
        case .birthday: "onboarding.birthday.title"
        case .starters: "onboarding.starters.title"
        case .welcome: "onboarding.welcome.title"
        default: unexpectedCase(self, fallback: "onboarding.welcome.title")
        }
    }

    var message: LocalizedStringKey {
        switch self {
        case .birthday: "onboarding.birthday.message"
        case .starters: "onboarding.starters.message"
        case .welcome: "onboarding.welcome.message"
        default: unexpectedCase(self, fallback: "onboarding.welcome.message")
        }
    }
}

extension OnboardingPrimaryAction {
    var title: LocalizedStringKey {
        switch self {
        case .createSelected: "onboarding.action.createSelected"
        case .startEmpty: "onboarding.action.startEmpty"
        case .next: "onboarding.action.next"
        default: unexpectedCase(self, fallback: "onboarding.action.next")
        }
    }
}

extension StarterEvent {
    /// The localized name, which also becomes the created event's title.
    var title: String {
        switch self {
        case .birthday: String(localized: "onboarding.starter.birthday")
        case .birthDate: String(localized: "onboarding.starter.birthDate")
        case .nextNewYear: String(localized: "onboarding.starter.newYear")
        case .nextMonth: String(localized: "onboarding.starter.nextMonth")
        default: unexpectedCase(self, fallback: String(localized: "onboarding.starter.nextMonth"))
        }
    }
}
