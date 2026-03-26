#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
TARGET_DIR="$SCRIPT_DIR/../target"
if [ -n "${JAVA21_HOME:-}" ]; then
  export JAVA_HOME="$JAVA21_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

JAR_PATH=$(ls -1t "$TARGET_DIR"/rill-*.jar 2>/dev/null | head -n 1 || true)

if [ -z "$JAR_PATH" ]; then
  echo "No packaged jar found under target/."
  echo "Run ./scripts/build.sh first."
  exit 1
fi

if [ -n "${JAVA_HOME:-}" ]; then
  exec "$JAVA_HOME/bin/java" -jar "$JAR_PATH" "$@"
fi

exec java -jar "$JAR_PATH" "$@"
