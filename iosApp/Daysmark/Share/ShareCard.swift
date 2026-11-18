import Shared
import SwiftUI

/// The card that is shared as an image: always light, at a fixed text size, so it reads the same
/// wherever it is sent.
struct ShareCard: View {
    let content: ShareUiStateContent

    var body: some View {
        VStack(spacing: Dimensions.spaceXLarge) {
            ZStack {
                // Multiplying with the flat page colour, as a filter: the drawing's near-white
                // backdrop becomes the page, with no blend-mode compositing involved.
                Artwork.image("share_botanical")
                    .resizable()
                    .scaledToFit()
                    .colorMultiply(DaysmarkColor.canvas.color)
                EventIconView(icon: content.icon, size: 112)
                    .offset(y: 8)
            }
            .frame(height: 180)
            VStack(spacing: Dimensions.spaceMedium) {
                Text(verbatim: content.title)
                    .textStyle(.textPrimary, .heroTitle)
                Text(verbatim: content.phrase)
                    .textStyle(content.dayCount.tint, .heroPhrase)
                Text(verbatim: content.dates)
                    .textStyle(.textSecondary, .supporting)
                if content.includesCategory {
                    Text(content.category.displayName)
                        .font(.footnote)
                        .foregroundStyle(.daysmark(.textSecondary))
                }
            }
            .fixedSize(horizontal: false, vertical: true)
            .padding(.horizontal, Dimensions.spaceSmall)
            VStack(spacing: Dimensions.spaceMedium) {
                Rectangle().fill(.daysmark(.separator)).frame(width: 32, height: 1)
                Text("sharing.card.reference \(content.asOf)")
                    .font(.caption2)
                    .foregroundStyle(.daysmark(.textSecondary))
            }
        }
        .multilineTextAlignment(.center)
        .padding(Dimensions.spaceXXLarge)
        .frame(width: 360)
        .frame(minHeight: 450)
        .background(.daysmark(.canvas))
        .environment(\.colorScheme, .light)
        .environment(\.dynamicTypeSize, .large)
    }
}

extension ShareUiStateContent {
    var phrase: String { phase.place(dayCount.phrase) }

    /// "Oct 25, 2026", or "Dec 28, 2026 – Jan 3, 2027" for a range.
    var dates: String {
        let style = Gregorian.style().year().month(.abbreviated).day()
        let startText = Gregorian.date(start, at: time).formatted(time == nil ? style : style.hour().minute())
        guard let end else { return startText }
        return String(localized: "sharing.card.range \(startText) \(end.date.formatted(style))")
    }

    var asOf: String {
        Gregorian.date(asOfDate, at: asOfTime).formatted(Gregorian.style(date: .long, time: .shortened))
    }
}
