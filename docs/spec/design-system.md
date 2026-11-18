# Design system

Both platforms draw each kind of value from one source, so they can't drift apart.

| Values | Source | Kept in sync by |
|---|---|---|
| Colours, light and dark | `resources/design-tokens.json`, generated into `Colors.xcassets` and `GeneratedColors.kt` | `scripts/generate-design-tokens.py --check` |
| Artwork | `resources/artwork/drawable-nodpi/*.webp`, used by both apps | One folder, no copies |
| Landscape geometry | `LandscapeScene` in shared Kotlin | `LandscapeSceneTest` |
| Spacing and type roles | Same names per platform (`Dimensions`, `DaysmarkFont` / `DaysmarkType`) | Review |

Generated files are never edited by hand. Artwork is WebP at quality 90, at least 40 dB PSNR
against the originals.

## Where UI code lives

| What | iOS | Android |
|---|---|---|
| Tokens: colours, type, dimensions, artwork | `iosApp/Common` | `designsystem/` |
| Views several screens use | `Daysmark/Components` | `designsystem/components/` |
| Text from Kotlin values | `Daysmark/Formatting` | `text/` |
| One screen | `Daysmark/<Screen>` | `<screen>/` |

## Platform conventions

Shared state, native behaviour:

| | iOS | Android |
|---|---|---|
| Add | Toolbar button | Floating action button |
| Row actions | Swipe: pin leading, delete trailing | Swipe: right pins, left deletes |
| Delete confirmation | Alert | `AlertDialog` |
| Text | Dynamic Type | Type scale in sp |

Dates use the locale's own pattern and show the year only when it isn't the current one.
