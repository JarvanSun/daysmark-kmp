import Shared
import SwiftUI

/// Confetti that falls once on the event's day. Positions come from the shared `Confetti`.
struct ConfettiView: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var start = Date.now
    @State private var isFinished = false
    private let confetti = Confetti(seed: Int32.random(in: 0...Int32.max))

    var body: some View {
        if !reduceMotion, !isFinished {
            TimelineView(.animation) { timeline in
                Canvas { context, size in
                    let elapsed = timeline.date.timeIntervalSince(start)
                    for piece in confetti.piecesAt(elapsedSeconds: elapsed, width: size.width, height: size.height) {
                        var pieceContext = context
                        pieceContext.translateBy(x: piece.centerX, y: piece.centerY)
                        pieceContext.rotate(by: .degrees(piece.rotationDegrees))
                        let side = piece.side
                        pieceContext.fill(
                            Path(CGRect(x: -side / 2, y: -side / 4, width: side, height: side / 2)),
                            with: .color(
                                Color(hue: piece.hue, saturation: Confetti.companion.SATURATION, brightness: 1)))
                    }
                }
            }
            .allowsHitTesting(false)
            .ignoresSafeArea()
            .accessibilityHidden(true)
            .task {
                try? await Task.sleep(for: .seconds(confetti.lifetimeSeconds))
                isFinished = true
            }
        }
    }
}
