import Foundation
import Testing

/// The widget's Codable mirror against resources/fixtures/widget-timeline-v2.json, the file Kotlin's
/// `WidgetSnapshotFixtureTest` proves it writes: Swift must read exactly what Kotlin writes.
struct WidgetSnapshotDecodingTests {
    private func fixture() throws -> WidgetSnapshot {
        let url = try #require(
            Bundle(for: BundleToken.self).url(forResource: "widget-timeline-v2", withExtension: "json"))
        return try JSONDecoder().decode(WidgetSnapshot.self, from: Data(contentsOf: url))
    }

    @Test func decodesEveryEntryOfTheSharedFixture() throws {
        let snapshot = try fixture()
        #expect(snapshot.version == WidgetSnapshot.version)
        #expect(snapshot.entries.count == 7)
        #expect(snapshot.entries.last?.focus == nil)
    }

    @Test func everyCountKindMapsToItsDistance() throws {
        let distances = try fixture().entries.compactMap { $0.focus?.distance }
        #expect(
            distances == [
                .future(days: 3), .today, .hours(2, isFuture: false), .minutes(5, isFuture: true), .now,
                .past(days: 12),
            ])
    }

    @Test func everyPhaseMapsToItsPlacement() throws {
        let placements = try fixture().entries.compactMap { $0.focus?.placement }
        #expect(placements == [.beforeStart, .ongoing, .point, .point, .point, .ended])
    }

    @Test func instantsAreMillisecondsSince1970() throws {
        let first = try #require(try fixture().entries.first)
        #expect(first.startsAt == Date(timeIntervalSince1970: 1_795_017_600))
    }

    @Test func rejectsEveryInvalidSharedFixture() throws {
        let folder = try #require(
            Bundle(for: BundleToken.self).url(forResource: "widget-timeline-invalid", withExtension: nil))
        let files = try FileManager.default.contentsOfDirectory(at: folder, includingPropertiesForKeys: nil)
        #expect(files.count == 3)
        for file in files {
            #expect(throws: DecodingError.self, "\(file.lastPathComponent)") {
                try JSONDecoder().decode(WidgetSnapshot.self, from: Data(contentsOf: file))
            }
        }
    }

    @Test func aFileFromAnotherVersionIsIgnored() throws {
        let other = Data(#"{"version":1,"entries":[]}"#.utf8)
        let snapshot = try JSONDecoder().decode(WidgetSnapshot.self, from: other)
        #expect(snapshot.version != WidgetSnapshot.version)
    }
}

private final class BundleToken {}
