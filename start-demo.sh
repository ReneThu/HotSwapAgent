#!/usr/bin/env bash
#
# Single entry point for the code hot-swap demo.
#
# It pre-builds the demo artifacts, starts the backend service (codeRunner on :8081)
# and the Slidev presentation, and tears everything down cleanly on exit — including any
# JVMs the backend spawned for the live demo (port :8080).
#
set -euo pipefail

# --- Hardcoded JDK so the demo never depends on the ambient environment -------------------
JAVA_HOME="/home/marco_sussitz/.jdkman/jdks/temurin/21/OpenJDK21U-jdk_x64_linux_hotspot_21.0.9_10"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PRESENTATION_DIR="$REPO_ROOT/presentation/hotswapAgent"

BACKEND_PID=""
SLIDEV_PID=""

# Recursively terminate a process and all of its descendants using explicit PIDs.
kill_tree() {
  local pid="$1"
  [ -z "$pid" ] && return 0
  local child
  for child in $(ps -o pid= --ppid "$pid" 2>/dev/null); do
    kill_tree "$child"
  done
  kill "$pid" 2>/dev/null || true
}

cleanup() {
  # Ignore further Ctrl+C/TERM while we tear down, so a second keypress can't abort cleanup
  # halfway and leave orphans behind.
  trap '' INT TERM
  echo
  echo "==> Shutting down and cleaning up..."

  # Graceful stop first: SIGTERM the backend so its @PreDestroy hook tears down the demo tree
  # (Gradle, the demo JVM and the agent server on :8080), and stop Slidev.
  [ -n "$BACKEND_PID" ] && kill_tree "$BACKEND_PID"
  [ -n "$SLIDEV_PID" ] && kill_tree "$SLIDEV_PID"

  # Give the graceful shutdown a moment to run.
  sleep 3

  # Force-kill any straggler in either tree that ignored the graceful signal.
  [ -n "$BACKEND_PID" ] && kill_tree_force "$BACKEND_PID"
  [ -n "$SLIDEV_PID" ] && kill_tree_force "$SLIDEV_PID"

  # Final safety net: whatever happened to the process tree (e.g. a demo JVM that got
  # reparented to init and escaped the walk), make sure the demo ports are actually free.
  free_port 8080
  free_port 8081

  # Report the result so it is obvious during a talk that nothing was left running.
  local leftovers=""
  ss -ltn 2>/dev/null | grep -qE ':8080\b' && leftovers="$leftovers :8080"
  ss -ltn 2>/dev/null | grep -qE ':8081\b' && leftovers="$leftovers :8081"
  if [ -n "$leftovers" ]; then
    echo "==> WARNING: still bound:$leftovers" >&2
  else
    echo "==> All demo services stopped (:8080 and :8081 are free)."
  fi
  echo "==> Done."
}

kill_tree_force() {
  local pid="$1"
  [ -z "$pid" ] && return 0
  local child
  for child in $(ps -o pid= --ppid "$pid" 2>/dev/null); do
    kill_tree_force "$child"
  done
  kill -9 "$pid" 2>/dev/null || true
}

# Kill whatever process is listening on a given TCP port (SIGTERM, then SIGKILL). Used as a
# last-resort safety net for the demo's own ports so no orphaned JVM keeps a port bound.
free_port() {
  local port="$1"
  local pid
  for pid in $(ss -ltnpH 2>/dev/null | awk -v p=":$port\$" '$4 ~ p' | grep -oP 'pid=\K[0-9]+' | sort -u); do
    kill "$pid" 2>/dev/null || true
  done
  sleep 1
  for pid in $(ss -ltnpH 2>/dev/null | awk -v p=":$port\$" '$4 ~ p' | grep -oP 'pid=\K[0-9]+' | sort -u); do
    kill -9 "$pid" 2>/dev/null || true
  done
}

trap cleanup EXIT
trap 'exit 130' INT TERM

echo "==> Using JDK:"
"$JAVA_HOME/bin/java" -version

echo "==> Pre-building demo artifacts (makes the first Start fast and reliable)..."
cd "$REPO_ROOT"
# Build exactly what the demo needs: the root jar + agent/micronaut shadow jars for :runMain,
# and the codeRunner shadow jar for the backend. This avoids running the app during the build.
./gradlew --no-daemon -x test \
  :build :agent:shadowJar :micronaut:shadowJar :codeRunner:shadowJar

CODERUNNER_JAR="$(ls "$REPO_ROOT"/codeRunner/build/libs/codeRunner-*-all.jar | head -n1)"
if [ -z "$CODERUNNER_JAR" ]; then
  echo "ERROR: could not find the codeRunner shadow jar." >&2
  exit 1
fi

echo "==> Starting backend service (codeRunner) on http://localhost:8081 ..."
# setsid isolates the backend from the terminal's Ctrl+C so this script's trap stays in
# control of the teardown and can walk the full process tree.
setsid "$JAVA_HOME/bin/java" -jar "$CODERUNNER_JAR" &
BACKEND_PID=$!

echo "==> Waiting for backend to become ready..."
for _ in $(seq 1 30); do
  if curl -sf http://localhost:8081/output >/dev/null 2>&1; then
    echo "==> Backend is up."
    break
  fi
  sleep 1
done

echo "==> Starting Slidev presentation from $PRESENTATION_DIR ..."
cd "$PRESENTATION_DIR"
if [ ! -d node_modules ]; then
  echo "==> Installing presentation dependencies..."
  npm install
fi
setsid npm run dev &
SLIDEV_PID=$!

echo "==> Everything is running. Press Ctrl+C to stop and clean up."
wait "$SLIDEV_PID"
