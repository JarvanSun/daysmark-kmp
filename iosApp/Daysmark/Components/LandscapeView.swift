import Shared
import SwiftUI

/// Home's landscape: the past on the left, the future on the right, today as a dot. Every position
/// and toning value comes from the shared `LandscapeScene` and `LandscapeArtwork`, so Android draws
/// the same picture.
struct LandscapeView: View {
    @Environment(\.colorScheme) private var colorScheme
    let counts: LandscapeCounts

    var body: some View {
        GeometryReader { proxy in
            scene(
                LandscapeScene(
                    width: proxy.size.width, height: Dimensions.landscapeHeight,
                    pastCount: counts.past, futureCount: counts.future))
        }
        .frame(height: Dimensions.landscapeHeight)
        .clipped()
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            Text("landscape.accessibility \(Int(counts.past)) \(Int(counts.future))"))
    }

    private func scene(_ scene: LandscapeScene) -> some View {
        ZStack(alignment: .topLeading) {
            if let distant = scene.distantHill {
                croppedHill(distant)
                    .toned(distant.artwork, tint: .landscapeFutureGround)
                    .opacity(scene.distantHillOpacity(isDark: colorScheme == .dark))
                    .position(x: distant.centerX, y: scene.ground - distant.height / 2)
            }
            ForEach(Array(scene.trees.enumerated()), id: \.offset) { _, tree in
                treeImage(tree)
                    .scaleEffect(x: tree.isFlipped ? -1 : 1, y: 1)
                    .opacity(tree.opacity)
                    .position(x: tree.centerX, y: tree.baseline - tree.height / 2)
            }
            if let hill = scene.pastHill {
                hillView(hill, isPast: true).position(x: hill.centerX, y: scene.ground - hill.height / 2)
            }
            if let hill = scene.futureHill {
                hillView(hill, isPast: false).position(x: hill.centerX, y: scene.ground - hill.height / 2)
            }
            ForEach(Array(scene.leaves.enumerated()), id: \.offset) { _, leaf in
                Artwork.image(LandscapeArtwork.autumnLeaf.fileName)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .toned(.autumnLeaf, tint: .landscapePastFoliage)
                    .frame(width: leaf.size)
                    .rotationEffect(.degrees(leaf.rotationDegrees))
                    .opacity(leaf.opacity)
                    .position(x: leaf.centerX, y: leaf.centerY)
            }
            Rectangle()
                .fill(.daysmark(.separator))
                .frame(width: scene.width, height: 1 / displayScale)
                .position(x: scene.center, y: scene.ground)
            Circle()
                .fill(.daysmark(.tint))
                .frame(width: scene.todayMarkerSize, height: scene.todayMarkerSize)
                .position(x: scene.center, y: scene.ground)
        }
        .frame(width: scene.width, height: scene.height)
    }

    @Environment(\.displayScale) private var displayScale

    private func treeImage(_ tree: LandscapeScene.Tree) -> some View {
        Artwork.image(tree.artwork.fileName)
            .resizable()
            .frame(width: tree.imageWidth, height: tree.height)
            .frame(width: tree.visibleWidth, alignment: .leading)
            .clipped()
            .toned(tree.artwork, tint: tree.isPast ? .landscapePastFoliage : .landscapeFutureFoliage)
    }

    private func hillView(_ hill: LandscapeScene.Hill, isPast: Bool) -> some View {
        croppedHill(hill)
            .toned(hill.artwork, tint: isPast ? .landscapePastGround : .landscapeFutureGround)
            .clipShape(HillEnding(isPast: isPast))
    }

    private func croppedHill(_ hill: LandscapeScene.Hill) -> some View {
        let crop = hill.artwork.crop(visibleHeight: hill.height)
        return Artwork.image(hill.artwork.fileName)
            .resizable()
            .frame(width: hill.width, height: crop.imageHeight)
            .offset(y: crop.offsetY)
            .frame(width: hill.width, height: hill.height, alignment: .top)
            .clipped()
    }
}

/// The future hill's rounded right end; the past hill runs off the leading edge.
nonisolated private struct HillEnding: Shape {
    let isPast: Bool

    func path(in rect: CGRect) -> Path {
        guard !isPast else { return Path(rect) }
        var path = Path()
        path.move(to: .zero)
        path.addLine(to: CGPoint(x: rect.width * 0.84, y: 0))
        path.addCurve(
            to: CGPoint(x: rect.width, y: rect.height),
            control1: CGPoint(x: rect.width * 0.91, y: rect.height * 0.1),
            control2: CGPoint(x: rect.width * 0.94, y: rect.height * 0.98))
        path.addLine(to: CGPoint(x: 0, y: rect.height))
        path.closeSubpath()
        return path
    }
}

extension View {
    /// The shared toning: saturation and contrast from `LandscapeArtwork`, then the side's colour.
    fileprivate func toned(_ artwork: LandscapeArtwork, tint: DaysmarkColor) -> some View {
        saturation(artwork.saturation).contrast(artwork.contrast).colorMultiply(tint.color)
    }
}
