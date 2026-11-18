import Shared
import SwiftUI

/// The focused event at the top of Home: the pinned one, or the nearest.
struct FocusCard: View {
    let row: EventRow

    var body: some View {
        VStack(alignment: .leading, spacing: Dimensions.spaceSmall) {
            Text(verbatim: row.title)
                .textStyle(.textPrimary, .heroTitle)
                .fixedSize(horizontal: false, vertical: true)
            DayCountText(row: row, font: .heroPhrase)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, Dimensions.spaceSmall)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(row.spokenDescription)
    }
}

/// A distance phrase in its direction's colour; digits roll when the day changes.
struct DayCountText: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let row: EventRow
    let font: DaysmarkFont

    var body: some View {
        Text(verbatim: row.phrase)
            .textStyle(row.dayCount.tint, font)
            .monospacedDigit()
            .contentTransition(.numericText(countsDown: row.dayCount.countsDown))
            .animation(reduceMotion ? nil : .snappy, value: row.phrase)
    }
}
