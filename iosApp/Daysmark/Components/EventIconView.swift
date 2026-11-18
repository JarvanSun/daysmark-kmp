import Shared
import SwiftUI

/// An event's illustrated icon (see `IconArtworkView`, which the widget shares).
struct EventIconView: View {
    let icon: EventIcon
    let size: CGFloat

    var body: some View {
        IconArtworkView(artwork: IconArtwork(storageKey: icon.storageKey), size: size)
    }
}
