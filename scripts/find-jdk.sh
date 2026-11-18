# Sourced by scripts that start Gradle outside a login shell (Xcode, scripts/verify.sh), which
# don't read ~/.zshrc. Resolves JAVA_HOME: as set, then the JDK bundled with Android Studio (what
# the IDE runs Gradle on), then Homebrew's openjdk. gradle/gradle-daemon-jvm.properties then picks
# the daemon's JDK; this one only has to start the wrapper.
if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/java" ]]; then
  for candidate in \
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
    "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"; do
    if [[ -x "$candidate/bin/java" ]]; then export JAVA_HOME="$candidate"; break; fi
  done
fi
if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/java" ]]; then
  echo "error: No JDK found. Install one (Android Studio, or brew install openjdk) or set JAVA_HOME." >&2
  exit 1
fi
