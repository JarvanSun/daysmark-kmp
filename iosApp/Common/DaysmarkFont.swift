import SwiftUI

/// Text styles, all built on Dynamic Type text styles so they scale with the user's setting.
enum DaysmarkFont {
    case pageTitle
    case pageSubtitle
    case heroTitle
    case heroPhrase
    case eventTitle
    case rowPhrase
    case rowSecondary
    case filterLabel
    case filterLabelSelected
    case placeholderTitle
    case noticeTitle
    case supporting
    case buttonLabel
    case linkLabel
    case disclosure
    case widgetTitle
    case widgetPhrase
    case widgetCaption

    var font: Font {
        switch self {
        case .pageTitle: .largeTitle.weight(.bold)
        case .pageSubtitle: .body
        case .heroTitle: .title.weight(.semibold)
        case .heroPhrase: .title2.weight(.semibold)
        case .eventTitle, .buttonLabel, .noticeTitle: .body.weight(.medium)
        case .rowPhrase, .linkLabel: .subheadline.weight(.medium)
        case .rowSecondary, .supporting, .filterLabel: .subheadline
        case .filterLabelSelected: .subheadline.weight(.semibold)
        case .placeholderTitle: .title3.weight(.semibold)
        case .disclosure: .footnote.weight(.semibold)
        case .widgetTitle: .headline
        case .widgetPhrase: .headline.weight(.medium)
        case .widgetCaption: .caption
        }
    }
}

extension View {
    func textStyle(_ color: DaysmarkColor, _ font: DaysmarkFont) -> some View {
        self.font(font.font).foregroundStyle(color.color)
    }
}
