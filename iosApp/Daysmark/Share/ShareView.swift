import KMPObservableViewModelSwiftUI
import Shared
import SwiftUI

/// Shares an event as a rendered card through the system share sheet.
struct ShareView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.displayScale) private var displayScale
    @StateViewModel private var viewModel: ShareViewModel

    /// The ViewModel is built only the first time this screen appears: `StateViewModel` keeps it
    /// afterwards, so a parent re-rendering never creates (and leaks) another one.
    init(viewModel: @autoclosure @escaping () -> ShareViewModel) {
        _viewModel = StateViewModel(wrappedValue: viewModel())
    }

    var body: some View {
        NavigationStack {
            Group {
                if let content = viewModel.uiState as? ShareUiStateContent {
                    panel(content)
                } else if viewModel.uiState is ShareUiStateFailed {
                    LoadFailedView { viewModel.onAction(action: ShareActionRetry.shared) }
                } else if viewModel.uiState is ShareUiStateMissing {
                    ContentUnavailableView("sharing.screen.unavailable", systemImage: "calendar.badge.exclamationmark")
                }
            }
            .background(.daysmark(.canvas))
            .navigationTitle("sharing.screen.title")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("sharing.screen.done") { dismiss() }
                }
            }
        }
        .tint(.daysmark(.tint))
    }

    /// The card as an image, for the share sheet.
    @State private var rendered: Image?

    private func panel(_ content: ShareUiStateContent) -> some View {
        VStack(spacing: Dimensions.spaceXLarge) {
            ScrollView {
                ShareCard(content: content)
                    .clipShape(.rect(cornerRadius: Dimensions.cardCornerRadius))
                    .overlay(RoundedRectangle(cornerRadius: Dimensions.cardCornerRadius).stroke(.daysmark(.separator)))
                    .padding(.top, Dimensions.spaceMedium)
                    .accessibilityElement(children: .combine)
            }
            .scrollBounceBehavior(.basedOnSize)
            Toggle(
                "sharing.panel.includeCategory",
                isOn: Binding(
                    get: { content.includesCategory },
                    set: { viewModel.onAction(action: ShareActionSetIncludesCategory(includes: $0)) }))
            if let image = rendered {
                ShareLink(
                    item: image,
                    preview: SharePreview(content.title, image: image)
                ) {
                    Label("sharing.panel.share", systemImage: "square.and.arrow.up")
                        .font(DaysmarkFont.buttonLabel.font)
                        .frame(maxWidth: .infinity, minHeight: Dimensions.primaryButtonHeight)
                }
                .buttonStyle(.glassProminent)
            }
        }
        .padding(.horizontal, Dimensions.spaceXLarge)
        .padding(.bottom, Dimensions.spaceMedium)
        // Rendered again only when the card changes (the category toggle), not on every redraw.
        .task(id: content) { rendered = render(content) }
    }

    /// The card as an image at the screen's scale, rendered off screen.
    private func render(_ content: ShareUiStateContent) -> Image? {
        let renderer = ImageRenderer(content: ShareCard(content: content))
        renderer.scale = displayScale
        return renderer.uiImage.map(Image.init(uiImage:))
    }
}
