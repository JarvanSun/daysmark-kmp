# Specs

How Daysmark is built and the rules every change follows. Where a rule is enforced by a check,
the check is named next to it.

| Spec | Covers |
|---|---|
| [product.md](product.md) | What the app does and the rules behind it |
| [architecture.md](architecture.md) | Modules, dependency rules, DI, persistence |
| [presentation.md](presentation.md) | The ViewModel contract and failure handling |
| [ios-interop.md](ios-interop.md) | The Kotlin framework in Swift, the Xcode project |
| [design-system.md](design-system.md) | Tokens, artwork, platform conventions |
| [quality.md](quality.md) | Gates, tests and conventions |
| [build.md](build.md) | Toolchain, upgrade rules, commands |

A rule changes together with its check: change both, show the check fails on a violation, then
remove the violation.
