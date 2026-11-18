import Shared
import SwiftUI

/// One row of category filters with "All" first. The selected one is tinted and bold.
struct FilterBar: View {
    let filters: [CategoryFilter]
    let onSelect: (EventCategory?) -> Void

    var body: some View {
        ScrollView(.horizontal) {
            HStack(spacing: Dimensions.spaceMedium) {
                ForEach(filters, id: \.self) { filter in
                    Button {
                        onSelect(filter.category)
                    } label: {
                        Text(filter.category?.displayName ?? String(localized: "home.filter.all"))
                            .textStyle(
                                filter.isSelected ? .tint : .textSecondary,
                                filter.isSelected ? .filterLabelSelected : .filterLabel
                            )
                            .frame(minWidth: Dimensions.minimumTapTarget, minHeight: Dimensions.minimumTapTarget)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(filter.isSelected ? .isSelected : [])
                }
            }
        }
        .scrollIndicators(.hidden)
    }
}

extension EventCategory {
    var displayName: String {
        switch self {
        case .birthday: String(localized: "category.birthday")
        case .anniversary: String(localized: "category.anniversary")
        default: String(localized: "category.important")
        }
    }
}
