#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
TARGET_DIR="$SCRIPT_DIR/../target"

JAR_PATH=$(ls -1t "$TARGET_DIR"/rill-*.jar 2>/dev/null | head -n 1 || true)

if [ -z "$JAR_PATH" ]; then
  echo "No packaged jar found under target/."
  echo "Run ./mvnw -DskipTests package first."
  exit 1
fi

exec java -jar "$JAR_PATH" "$@"
