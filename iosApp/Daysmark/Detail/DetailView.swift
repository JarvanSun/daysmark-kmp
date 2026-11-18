import KMPObservableViewModelSwiftUI
import Shared
import SwiftUI

/// One event: the large count the user can cycle through units, the ticking line, pin, edit, share.
struct DetailView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @StateViewModel private var viewModel: DetailViewModel
    let router: AppRouter

    /// The ViewModel is built only the first time this screen appears: `StateViewModel` keeps it
    /// afterwards, so a parent re-rendering never creates (and leaks) another one.
    init(viewModel: @autoclosure @escaping () -> DetailViewModel, router: AppRouter) {
        _viewModel = StateViewModel(wrappedValue: viewModel())
        self.router = router
    }

    var body: some View {
        ZStack {
            if let content = viewModel.uiState as? DetailUiStateContent {
                countdown(content)
                    .toolbar { toolbar(content) }
                    .sensoryFeedback(.selection, trigger: content.count.unit)
                    .failureAlert("failure.pin.title", isPresented: content.pinFailed) {
                        viewModel.onAction(action: DetailActionDismissFailure.shared)
                    }
            } else if viewModel.uiState is DetailUiStateFailed {
                LoadFailedView { viewModel.onAction(action: DetailActionRetry.shared) }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        // `initial`: a screen opened on an event that is already gone leaves at once.
        .onChange(of: viewModel.uiState is DetailUiStateMissing, initial: true) { _, missing in
            if missing { dismiss() }
        }
    }

    private func countdown(_ content: DetailUiStateContent) -> some View {
        VStack(spacing: 0) {
            VStack(spacing: Dimensions.spaceCompact) {
                Text(verbatim: content.title)
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(.daysmark(.textPrimary))
                    .multilineTextAlignment(.center)
                Text(verbatim: content.formattedDate)
                    .textStyle(.textSecondary, .supporting)
            }
            .padding(.top, Dimensions.detailTopInset)
            Spacer(minLength: Dimensions.spaceXLarge)
            Button {
                viewModel.onAction(action: DetailActionCycleUnit.shared)
            } label: {
                DetailCountView(content: content)
            }
            .buttonStyle(.plain)
            .disabled(!content.canSwitchUnit)
            .accessibilityHint(Text("detail.count.switchUnitHint"))
            if let ticker = content.ticker {
                Text(verbatim: ticker.formatted)
                    .font(.system(.title3, design: .monospaced, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(.daysmark(.textSecondary))
                    .contentTransition(.numericText())
                    .animation(reduceMotion ? nil : .snappy, value: ticker.seconds)
                    .padding(.top, Dimensions.spaceXLarge)
                    .accessibilityHidden(true)
            }
            if content.canSwitchUnit {
                Text("detail.count.switchUnitHint")
                    .font(.footnote)
                    .foregroundStyle(.daysmark(.textTertiary))
                    .padding(.top, Dimensions.spaceMedium)
                    .accessibilityHidden(true)
            }
            Spacer(minLength: Dimensions.spaceXLarge)
            Button {
                router.share(content.id)
            } label: {
                Label("detail.actions.share", systemImage: "square.and.arrow.up")
                    .font(DaysmarkFont.buttonLabel.font)
                    .frame(maxWidth: .infinity, minHeight: Dimensions.primaryButtonHeight)
            }
            .buttonStyle(.glassProminent)
            .tint(.daysmark(.tint))
        }
        .padding(.horizontal, Dimensions.spaceXLarge)
        .padding(.bottom, Dimensions.spaceMedium)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(alignment: .top) {
            LinearGradient(
                colors: [content.tint.color.opacity(Dimensions.detailWashOpacity), content.tint.color.opacity(0)],
                startPoint: .top, endPoint: .center
            )
            .ignoresSafeArea()
        }
        .background(.daysmark(.canvas))
        .overlay {
            if content.isTheDay { ConfettiView() }
        }
    }

    @ToolbarContentBuilder
    private func toolbar(_ content: DetailUiStateContent) -> some ToolbarContent {
        ToolbarItem(placement: .primaryAction) {
            Button {
                viewModel.onAction(action: DetailActionTogglePin.shared)
            } label: {
                Label(
                    content.isPinned ? "home.row.unpin" : "home.row.pin",
                    systemImage: content.isPinned ? "pin.fill" : "pin")
            }
        }
        ToolbarSpacer(.fixed, placement: .primaryAction)
        ToolbarItem(placement: .primaryAction) {
            Button("detail.toolbar.edit") { router.edit(content.id) }
        }
    }
}

/// The large number and its caption: "547 · days since", or "Today".
private struct DetailCountView: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric(relativeTo: .largeTitle) private var numberSize: CGFloat = Dimensions.detailNumberSize
    let content: DetailUiStateContent

    var body: some View {
        let count = content.count
        VStack(spacing: Dimensions.spaceCompact) {
            Group {
                if count.direction == .today {
                    Text("detail.count.today")
                } else {
                    Text(count.value, format: .number)
                }
            }
            .font(.system(size: numberSize, weight: .heavy, design: .rounded))
            .monospacedDigit()
            .foregroundStyle(.daysmark(content.tint))
            .lineLimit(1)
            .minimumScaleFactor(0.3)
            .contentTransition(.numericText(countsDown: count.direction == .upcoming))
            Text(count.caption)
                .font(.title3.weight(.semibold))
                .foregroundStyle(.daysmark(.textSecondary))
                .contentTransition(.opacity)
            if count.remainderDays > 0 {
                Text("detail.count.remainder \(Int(count.remainderDays))")
                    .textStyle(.textSecondary, .supporting)
            }
        }
        .animation(reduceMotion ? nil : .smooth(duration: 0.5), value: count.unit)
        .contentShape(.rect)
    }
}

extension DetailUiStateContent {
    var tint: DaysmarkColor { count.direction == .past ? .past : .upcoming }

    /// "2025年4月8日 周二", "Tue, Apr 8, 2025", with the time when there is one.
    var formattedDate: String {
        var style = Gregorian.style().year().month(.abbreviated).day().weekday(.abbreviated)
        if time != nil { style = style.hour().minute() }
        return Gregorian.date(date, at: time).formatted(style)
    }
}

extension DetailCount {
    /// "days since", "还有 · 天": direction and unit, singular or plural. Separate "day" and
    /// "days" keys rather than a plural variation: the caption doesn't show the number (it is
    /// above), and String Catalogs require a plural variation to reference it.
    var caption: LocalizedStringKey {
        if direction == .today { return "detail.count.caption.today" }
        let side = direction == .past ? "past" : "upcoming"
        let unitName: String =
            switch unit {
            case .weeks: "week"
            case .hours: "hour"
            case .minutes: "minute"
            default: "day"
            }
        // Built as a String first: an interpolated LocalizedStringKey literal would look up a
        // "%@" format key instead of this one.
        let key: String = "detail.count.caption.\(side).\(unitName)\(value == 1 ? "" : "s")"
        return LocalizedStringKey(key)
    }
}

extension Elapsed {
    /// "547d 9h 41m 0s" in the user's language, as Foundation writes durations.
    var formatted: String {
        let total = ((Int(days) * 24 + Int(hours)) * 60 + Int(minutes)) * 60 + Int(seconds)
        return Duration.seconds(total).formatted(
            .units(allowed: [.days, .hours, .minutes, .seconds], width: .narrow, zeroValueUnits: .show(length: 1)))
    }
}
