import SwiftUI
import UIKit
import WidgetKit

/// The shared artwork in `resources/artwork`, bundled as WebP files; Android uses the same files.
enum Artwork {
    static func image(_ fileName: String) -> Image {
        Image(uiImage: uiImage(fileName))
    }

    static func uiImage(_ fileName: String) -> UIImage {
        guard let image = UIImage(named: "\(fileName).webp") else {
            assertionFailure("Missing artwork \(fileName).webp")
            return UIImage()
        }
        return image
    }
}

/// An event icon's drawing, found by the key the event stores (`EventIcon.storageKey` in Kotlin),
/// so the app and the widget, which has no Kotlin, draw the same picture.
struct IconArtwork: Equatable {
    let fileName: String
    /// Evens out the drawings' different visual weights, so all icons look the same size.
    let opticalScale: CGFloat

    init(storageKey: String) {
        (fileName, opticalScale) =
            switch storageKey {
            case "star": ("event_appearance_star", 1.169)
            case "gift": ("event_appearance_gift", 1.052)
            case "heart": ("event_appearance_heart", 1.089)
            case "airplane": ("event_appearance_airplane", 1.248)
            case "graduationcap": ("event_appearance_graduation", 0.934)
            case "birthday.cake": ("event_appearance_cake", 0.954)
            case "briefcase": ("event_appearance_briefcase", 0.921)
            case "house": ("event_appearance_house", 0.946)
            case "figure.run": ("event_appearance_running", 0.894)
            case "book": ("event_appearance_book", 0.947)
            default: ("event_appearance_calendar", 0.914)
            }
    }
}

/// An event's illustrated icon, toned down slightly so it sits quietly next to text.
struct IconArtworkView: View {
    @Environment(\.colorScheme) private var colorScheme
    let artwork: IconArtwork
    let size: CGFloat

    var body: some View {
        Artwork.image(artwork.fileName)
            .resizable()
            // On a tinted or clear Home Screen the drawing stays a drawing, in grey.
            .widgetAccentedRenderingMode(.desaturated)
            .scaledToFit()
            .saturation(colorScheme == .dark ? 0.72 : 0.82)
            .brightness(colorScheme == .dark ? 0.08 : 0)
            .frame(width: size, height: size)
            .scaleEffect(artwork.opticalScale)
            .accessibilityHidden(true)
    }
}
