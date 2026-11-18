import Shared
import SwiftUI

/// One event in Home's list: icon, title, phrase and date. Long titles wrap to two lines.
struct EventRowView: View {
    let row: EventRow

    var body: some View {
        HStack(spacing: Dimensions.spaceMedium) {
            EventIconView(icon: row.icon, size: Dimensions.eventIconSize)
                .frame(width: Dimensions.eventIconSize)
            VStack(alignment: .leading, spacing: Dimensions.rowLineSpacing) {
                Text(verbatim: row.title)
                    .textStyle(.textPrimary, .eventTitle)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
                DayCountText(row: row, font: .rowPhrase)
                Text(verbatim: row.formattedDate)
                    .textStyle(.textSecondary, .rowSecondary)
            }
            Spacer(minLength: Dimensions.spaceMedium)
            Image(systemName: "chevron.forward")
                .textStyle(.textTertiary, .disclosure)
                .accessibilityHidden(true)
        }
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(row.spokenDescription)
    }
}
