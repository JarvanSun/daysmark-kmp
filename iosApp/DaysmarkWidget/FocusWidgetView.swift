import SwiftUI
import WidgetKit

struct FocusWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: FocusEntry

    var body: some View {
        content
            .widgetURL(entry.focus.map { DeepLink.event(id: $0.id) }?.url ?? DeepLink.newEvent.url)
            .containerBackground(for: .widget) {
                if family == .systemSmall { DaysmarkColor.canvas.color }
            }
    }

    @ViewBuilder private var content: some View {
        switch (entry.focus, family) {
        case let (focus?, .accessoryRectangular): FocusRectangular(focus: focus)
        case let (focus?, .accessoryInline): Text("widget.focus.inline \(focus.title) \(focus.phrase)")
        case let (focus?, _): FocusCard(focus: focus)
        case (nil, .accessoryInline): Text("widget.empty.title")
        case (nil, _): FocusEmpty(isCompact: family != .systemSmall)
        }
    }
}

/// Small: the icon at the top, the title and the distance at the bottom.
private struct FocusCard: View {
    let focus: WidgetSnapshot.Focus

    var body: some View {
        VStack(alignment: .leading, spacing: Dimensions.spaceCompact) {
            IconArtworkView(artwork: focus.artwork, size: Dimensions.eventIconSize)
            Spacer(minLength: Dimensions.spaceSmall)
            Text(focus.title)
                .textStyle(.textPrimary, .widgetTitle)
                .lineLimit(2)
            Text(verbatim: focus.phrase)
                .textStyle(focus.distance.tint, .widgetPhrase)
                .monospacedDigit()
                .lineLimit(2)
                .minimumScaleFactor(0.8)
                .widgetAccentable()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }
}

/// Lock Screen: system colours, the title and the distance.
private struct FocusRectangular: View {
    let focus: WidgetSnapshot.Focus

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(focus.title)
                .font(DaysmarkFont.widgetTitle.font)
                .lineLimit(2)
            Text(verbatim: focus.phrase)
                .font(DaysmarkFont.widgetCaption.font)
                .lineLimit(1)
                .widgetAccentable()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }
}

/// No events yet: tapping opens a new one.
private struct FocusEmpty: View {
    let isCompact: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: Dimensions.spaceCompact) {
            if !isCompact {
                IconArtworkView(artwork: IconArtwork(storageKey: "calendar"), size: Dimensions.eventIconSize)
                Spacer(minLength: Dimensions.spaceSmall)
            }
            Text("widget.empty.title")
                .font(DaysmarkFont.widgetTitle.font)
                .foregroundStyle(isCompact ? Color.primary : DaysmarkColor.textPrimary.color)
            Text("widget.empty.action")
                .font((isCompact ? DaysmarkFont.widgetCaption : .rowSecondary).font)
                .foregroundStyle(isCompact ? Color.secondary : DaysmarkColor.textSecondary.color)
        }
        .frame(maxWidth: .infinity, maxHeight: isCompact ? nil : .infinity, alignment: .leading)
    }
}

#Preview("Small", as: .systemSmall) {
    FocusWidget()
} timeline: {
    FocusEntry(date: .now, focus: .placeholder)
    FocusEntry(date: .now, focus: nil)
}
