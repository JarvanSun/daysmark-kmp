import Shared
import SwiftUI

/// What Home shows instead of rows: the first-date invitation, or a reset for an empty filter.
struct HomeEmptyView: View {
    @Environment(\.colorScheme) private var colorScheme
    let placeholder: HomePlaceholder
    let onAdd: () -> Void
    let onResetFilters: () -> Void

    var body: some View {
        switch placeholder {
        case .noMatches: noMatches
        default: noEvents
        }
    }

    private var noEvents: some View {
        VStack(spacing: Dimensions.spaceXLarge) {
            firstDateArtwork
                .frame(maxWidth: Dimensions.emptyArtworkMaximumWidth)
                .accessibilityHidden(true)
            message(
                title: "home.empty.title", detail: "home.empty.description",
                titleFont: .placeholderTitle, spacing: Dimensions.placeholderMessageSpacing)
            Button(action: onAdd) {
                Text("home.empty.create")
                    .font(DaysmarkFont.buttonLabel.font)
                    .padding(.horizontal, Dimensions.spaceMedium)
                    .frame(minHeight: Dimensions.minimumTapTarget)
            }
            .buttonStyle(.glassProminent)
            .tint(.daysmark(.tint))
        }
        .frame(maxWidth: .infinity)
        .padding(.top, Dimensions.placeholderTopInset)
        .padding(.bottom, Dimensions.spaceXXLarge)
    }

    private var noMatches: some View {
        VStack(spacing: Dimensions.spaceMedium) {
            leaves
                .frame(width: Dimensions.filteredArtworkWidth, height: Dimensions.filteredArtworkHeight)
                .accessibilityHidden(true)
            message(
                title: "home.filter.empty.title", detail: "home.filter.empty.description",
                titleFont: .noticeTitle, spacing: Dimensions.spaceSmall
            )
            .accessibilityElement(children: .combine)
            Button(action: onResetFilters) {
                Text("home.filter.reset")
                    .font(DaysmarkFont.linkLabel.font)
                    .foregroundStyle(.daysmark(.tint))
                    .frame(minWidth: Dimensions.minimumTapTarget, minHeight: Dimensions.minimumTapTarget)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, Dimensions.spaceXXLarge)
    }

    private func message(
        title: LocalizedStringKey, detail: LocalizedStringKey, titleFont: DaysmarkFont, spacing: CGFloat
    ) -> some View {
        VStack(spacing: spacing) {
            Text(title).textStyle(.textPrimary, titleFont).accessibilityAddTraits(.isHeader)
            Text(detail).textStyle(.textSecondary, .supporting)
        }
        .multilineTextAlignment(.center)
        .fixedSize(horizontal: false, vertical: true)
    }

    /// Multiplied with the page in light mode, screened onto it in dark mode, as the original.
    @ViewBuilder
    private var firstDateArtwork: some View {
        if colorScheme == .dark {
            Artwork.image("home_first_date_dark").resizable().scaledToFit()
                .blendMode(.screen)
                .blendedOnCanvas()
        } else {
            Artwork.image("home_first_date").resizable().scaledToFit()
                .colorMultiply(DaysmarkColor.canvas.color)
        }
    }

    @ViewBuilder
    private var leaves: some View {
        let image = Artwork.image("home_empty_paper_leaves").resizable().scaledToFit()
        if colorScheme == .dark {
            image.saturation(0).colorInvert().colorMultiply(.daysmark(.tint)).blendMode(.screen)
                .blendedOnCanvas()
        } else {
            image.saturation(0.8).colorMultiply(DaysmarkColor.canvas.color)
        }
    }
}

extension View {
    /// Puts the page colour under a blended illustration, in one compositing group, so the blend
    /// mixes with the page and the drawing's near-white backdrop disappears.
    func blendedOnCanvas() -> some View {
        background(DaysmarkColor.canvas.color).compositingGroup()
    }
}
