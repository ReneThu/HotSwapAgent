#!/usr/bin/env bash
#
# Metaspace-leak-via-HotSwap demo.
#
# Builds a tiny Java agent, pre-compiles ~5000 versions of demo.Leaker (each differing only by the
# printed swap label), then runs a fixed-duration loop that reflectively asks the agent to redefine
# Leaker to the next version on every iteration. A Java Flight Recording capturing EVERYTHING
# (settings=profile, plus Native Memory Tracking so the Metaspace size is recorded) is written to
# metaspace-leak.jfr on exit.
#
# Usage:
#   ./run-leak.sh [DURATION_MS] [NUM_VERSIONS] [PRINT_EVERY] [TARGET_MB]
#   ./run-leak.sh --regen [DURATION_MS] [NUM_VERSIONS] [PRINT_EVERY] [TARGET_MB]  # rebuild classes
#
# By default it leaks until Metaspace reaches TARGET_MB (2048 = ~2 GB), using DURATION_MS purely as a
# safety cap. Set TARGET_MB=0 to run for the full fixed duration instead.
#
# Environment:
#   JAVA_HOME_21   Path to a Temurin/OpenJDK 21 JDK (falls back to a hard-coded default).
#
set -euo pipefail

# --- JDK selection ------------------------------------------------------------------------------
DEFAULT_JDK="/home/marco_sussitz/.jdkman/jdks/temurin/21/OpenJDK21U-jdk_x64_linux_hotspot_21.0.9_10"
JAVA_HOME="${JAVA_HOME_21:-$DEFAULT_JDK}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
JAVA="$JAVA_HOME/bin/java"
JAVAC="$JAVA_HOME/bin/javac"
JAR="$JAVA_HOME/bin/jar"

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$HERE"

# --- Arguments ----------------------------------------------------------------------------------
REGEN=0
if [ "${1:-}" = "--regen" ]; then
  REGEN=1
  shift
fi
DURATION_MS="${1:-600000}"
NUM_VERSIONS="${2:-40000}"
PRINT_EVERY="${3:-1}"
TARGET_MB="${4:-2048}"

AGENT_JAR="build/agent.jar"
GEN_DIR="build/gen"
SWAPS_DIR="swaps"
JFR_FILE="metaspace-leak.jfr"

echo "==> Using JDK: $JAVA_HOME"
"$JAVA" -version

# --- 1. Build the agent jar (once) --------------------------------------------------------------
if [ ! -f "$AGENT_JAR" ]; then
  echo "==> Building agent jar..."
  rm -rf build/agentclasses
  mkdir -p build/agentclasses
  "$JAVAC" -d build/agentclasses agent/MetaspaceLeakAgent.java
  "$JAR" --create --file "$AGENT_JAR" --manifest agent/manifest.mf -C build/agentclasses .
  echo "==> Agent jar: $AGENT_JAR"
else
  echo "==> Agent jar already present: $AGENT_JAR"
fi

# --- 2. Pre-compile the swap classes (once, unless --regen) -------------------------------------
NEED_GEN=$REGEN
if [ ! -d "$SWAPS_DIR" ] || [ ! -f "$SWAPS_DIR/00000/demo/Leaker.class" ]; then
  NEED_GEN=1
fi
# Regenerate if the requested version count differs from what is on disk.
if [ "$NEED_GEN" -eq 0 ]; then
  EXPECTED_LAST="$SWAPS_DIR/$(printf '%05d' $((NUM_VERSIONS - 1)))/demo/Leaker.class"
  ONE_PAST="$SWAPS_DIR/$(printf '%05d' "$NUM_VERSIONS")/demo/Leaker.class"
  if [ ! -f "$EXPECTED_LAST" ] || [ -f "$ONE_PAST" ]; then
    NEED_GEN=1
  fi
fi

if [ "$NEED_GEN" -eq 1 ]; then
  echo "==> Generating $NUM_VERSIONS swap classes into $SWAPS_DIR/ ..."
  mkdir -p "$GEN_DIR"
  "$JAVAC" -d "$GEN_DIR" app/Generator.java
  "$JAVA" -cp "$GEN_DIR" Generator app/Leaker.java.template "$SWAPS_DIR" "$NUM_VERSIONS"
else
  echo "==> Swap classes already present in $SWAPS_DIR/ (use --regen to rebuild)."
fi

# --- 3. Run the leak with a full-capture flight recording ---------------------------------------
rm -f "$JFR_FILE"
if [ "$TARGET_MB" -gt 0 ]; then
  echo "==> Starting leak until Metaspace ~${TARGET_MB}MB (cap ${DURATION_MS}ms); recording -> $JFR_FILE"
else
  echo "==> Starting leak for ${DURATION_MS}ms; flight recording -> $JFR_FILE"
fi
echo

# Only capture-related JVM args are used (no leak/GC tuning):
#   -javaagent                : provides the hot-swap (redefineClasses) capability.
#   -XX:NativeMemoryTracking  : lets the flight recording capture Metaspace/Class native size.
#   -XX:StartFlightRecording  : produces the deliverable .jfr; settings=profile captures everything.
set +e
"$JAVA" \
  -XX:NativeMemoryTracking=summary \
  -XX:StartFlightRecording=name=metaspaceleak,filename="$JFR_FILE",settings=profile,dumponexit=true \
  -javaagent:"$AGENT_JAR" \
  -cp "$SWAPS_DIR/00000" \
  demo.Leaker "$DURATION_MS" "$SWAPS_DIR" "$NUM_VERSIONS" "$PRINT_EVERY" "$TARGET_MB"
RC=$?
set -e

echo
if [ -f "$JFR_FILE" ]; then
  REDEFS="$("$JAVA_HOME/bin/jfr" summary "$JFR_FILE" | awk '/jdk.RedefineClasses/{print $2}')"
  echo "==> Flight recording written: $HERE/$JFR_FILE"
  echo "==> Redefinitions recorded (jdk.RedefineClasses): ${REDEFS:-0}"
  echo
  echo "==> Metaspace committed over time (jdk.NativeMemoryUsage, type=Metaspace) - the leak curve:"
  "$JAVA_HOME/bin/jfr" print --events jdk.NativeMemoryUsage "$JFR_FILE" \
    | awk '/startTime =/{t=$3} /type = "Metaspace"/{f=1} f&&/committed =/{print "      "t"   committed = "$3" "$4; f=0}' \
    | awk 'NR%8==1{print} {last=$0} END{if (NR>0 && (NR-1)%8!=0) print last}'
  echo
  echo "==> Leaking class loader's metaspace over time (jdk.ClassLoaderStatistics, AppClassLoader):"
  "$JAVA_HOME/bin/jfr" print --events jdk.ClassLoaderStatistics "$JFR_FILE" \
    | awk '/startTime =/{t=$3} /AppClassLoader/{f=1} f&&/chunkSize =/{print "      "t"   chunkSize  = "$3" "$4; f=0}'
  echo
  echo "==> Inspect the full recording with:  $JAVA_HOME/bin/jfr print $JFR_FILE"
  echo "==> Or open $JFR_FILE in JDK Mission Control (Memory -> Native Memory / Metaspace)."
else
  echo "!! No flight recording produced (java exit code $RC)." >&2
fi

exit "$RC"
