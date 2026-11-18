#!/bin/zsh
# Xcode pre-build phase: builds the Kotlin "Shared" framework for the current configuration and SDK.
set -euo pipefail
source "$SRCROOT/../scripts/find-jdk.sh"

cd "$SRCROOT/.."
./gradlew :shared:embedAndSignAppleFrameworkForXcode
