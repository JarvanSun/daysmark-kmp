import Shared
import SwiftUI

/// The live preview at the top of the editor: "Mia's birthday · in 18 days", as a large number.
struct EditorPreviewRow: View {
    let title: String
    let preview: EditorPreview

    var body: some View {
        HStack(alignment: .center, spacing: Dimensions.spaceMedium) {
            Text(verbatim: caption)
                .textStyle(.textSecondary, .rowSecondary)
                .lineLimit(2)
            Spacer(minLength: Dimensions.spaceMedium)
            if let days {
                VStack(alignment: .trailing, spacing: 0) {
                    Text(days, format: .number)
                        .font(.system(.title, design: .rounded, weight: .heavy))
                        .foregroundStyle(.daysmark(preview.dayCount.tint))
                        .contentTransition(.numericText())
                    Text(days == 1 ? "editor.preview.unit.day" : "editor.preview.unit.days")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.daysmark(.textSecondary))
                }
            } else {
                Text(verbatim: preview.phase.place(preview.dayCount.phrase))
                    .font(.title3.weight(.heavy))
                    .foregroundStyle(.daysmark(.upcoming))
            }
        }
        .padding(Dimensions.spaceMedium)
        .background(.daysmark(.canvasSecondary), in: .rect(cornerRadius: 14))
        .animation(.snappy, value: days)
        .accessibilityElement(children: .combine)
    }

    private var name: String {
        title.trimmingCharacters(in: .whitespaces).isEmpty
            ? String(localized: "editor.preview.untitled") : title
    }

    private var days: Int? {
        if let future = preview.dayCount as? DayCountFuture { return Int(future.days) }
        if let past = preview.dayCount as? DayCountPast { return Int(past.days) }
        return nil
    }

    private var caption: String {
        if preview.dayCount is DayCountFuture { return String(localized: "editor.preview.upcoming \(name)") }
        if preview.dayCount is DayCountPast { return String(localized: "editor.preview.past \(name)") }
        return String(localized: "editor.preview.today \(name)")
    }
}
