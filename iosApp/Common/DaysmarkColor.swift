import SwiftUI

/// The colour tokens. The colour sets are generated from `resources/design-tokens.json`
/// (`scripts/generate-design-tokens.py`), the same source Android's colours come from.
enum DaysmarkColor: String {
    case canvas
    case canvasSecondary
    case textPrimary
    case textSecondary
    case textTertiary
    case textOnTint
    case separator
    case tint
    case tintSoft
    case upcoming
    case past
    case destructive
    case landscapePastGround
    case landscapeFutureGround
    case landscapePastFoliage
    case landscapeFutureFoliage

    var color: Color { Color(rawValue) }
}

extension ShapeStyle where Self == Color {
    static func daysmark(_ token: DaysmarkColor) -> Color { token.color }
}
