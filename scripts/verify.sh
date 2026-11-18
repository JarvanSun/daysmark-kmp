#!/bin/zsh
# The whole repository's gate, the same locally and in CI (docs/spec/quality.md).
#   scripts/verify.sh          everything
#   scripts/verify.sh kotlin   design tokens + ./gradlew check (tests on JVM, Android host and the
#                              iOS simulator, detekt, ktfmt, Konsist, lint, the framework API check)
#   scripts/verify.sh ios      swift-format, then the Xcode project's build and Swift tests
# The simulator comes from $IOS_TEST_DEVICE, else `daysmark.iosTestDevice` in gradle.properties.
# Build products go to $XCODE_BUILD_ROOT (default: Xcode's DerivedData), never into the repository.
set -euo pipefail
cd "${0:A:h}/.."
source scripts/find-jdk.sh

device=${IOS_TEST_DEVICE:-$(sed -n 's/^daysmark.iosTestDevice=//p' gradle.properties)}
derived="${XCODE_BUILD_ROOT:-$HOME/Library/Developer/Xcode/DerivedData}/DaysmarkKMP-verify"
xcodegen_version=$(cat iosApp/.xcodegen-version)

kotlin() {
  echo "== Design tokens"
  python3 scripts/generate-design-tokens.py --check
  echo "== ./gradlew check"
  ./gradlew check -Pdaysmark.iosTestDevice="$device" --continue
  echo "== Room schemas are committed"
  # The build exports the schema; one it changed or added that isn't committed fails here.
  if [[ -n "$(git status --porcelain -- core/data/schemas)" ]]; then
    git status --short -- core/data/schemas >&2
    echo "error: commit the exported Room schema (core/data/schemas)." >&2
    exit 1
  fi
}

ios() {
  echo "== swift-format"
  xcrun swift-format lint --strict --recursive \
    iosApp/Daysmark iosApp/Common iosApp/DaysmarkWidget iosApp/DaysmarkTests iosApp/DaysmarkWidgetTests
  echo "== XcodeGen $xcodegen_version"
  if [[ "$(xcodegen --version)" != *"$xcodegen_version"* ]]; then
    echo "error: XcodeGen $xcodegen_version is required (iosApp/.xcodegen-version)." >&2
    exit 1
  fi
  (cd iosApp && xcodegen generate --quiet)
  echo "== Xcode: build and Swift tests on $device"
  xcodebuild test -project iosApp/DaysmarkKMP.xcodeproj -scheme Daysmark \
    -destination "platform=iOS Simulator,name=$device" -derivedDataPath "$derived" \
    CODE_SIGNING_ALLOWED=NO -quiet
}

case "${1:-all}" in
  kotlin) kotlin ;;
  ios) ios ;;
  all) kotlin && ios ;;
  *) echo "usage: scripts/verify.sh [kotlin|ios]" >&2; exit 2 ;;
esac
echo "== All checks passed"
