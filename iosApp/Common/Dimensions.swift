import CoreGraphics

/// Spacing and sizes, in points. The same values as the Swift Daysmark app.
enum Dimensions {
    static let pageMargin: CGFloat = 20
    static let spaceCompact: CGFloat = 4
    static let spaceSmall: CGFloat = 8
    static let spaceMedium: CGFloat = 12
    static let spaceXLarge: CGFloat = 24
    static let spaceXXLarge: CGFloat = 32
    static let minimumTapTarget: CGFloat = 44
    /// The full-width button at the bottom of Detail, Share and onboarding.
    static let primaryButtonHeight: CGFloat = 50
    /// The share card's corners.
    static let cardCornerRadius: CGFloat = 20
    /// Detail's large number before Dynamic Type scaling (scaled relative to `.largeTitle`).
    static let detailNumberSize: CGFloat = 104
    /// Space above Detail's title, under the navigation bar.
    static let detailTopInset: CGFloat = 16
    /// How strongly Detail's direction colour washes the top of the screen.
    static let detailWashOpacity: Double = 0.14
    /// Onboarding's text column on wide screens (iPad), so lines stay readable.
    static let readableWidth: CGFloat = 560
    static let selectionMarkWidth: CGFloat = 28
    /// Separators in a selection list start where the text does.
    static let selectionMarkInset: CGFloat = selectionMarkWidth + spaceMedium
    static let rowLineSpacing: CGFloat = 2
    static let eventIconSize: CGFloat = 36
    static let landscapeHeight: CGFloat = 96
    static let placeholderTopInset: CGFloat = 48
    static let placeholderMessageSpacing: CGFloat = 10
    static let emptyArtworkMaximumWidth: CGFloat = 240
    static let filteredArtworkWidth: CGFloat = 164
    static let filteredArtworkHeight: CGFloat = 110
}
