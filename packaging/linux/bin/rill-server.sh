#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
APP_HOME=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
JAVA_EXE="$APP_HOME/runtime/bin/java"
TARGET_JAR="$APP_HOME/server/rill-server.jar"

if [ ! -x "$JAVA_EXE" ]; then
  echo "Missing bundled runtime: $JAVA_EXE"
  exit 1
fi

if [ ! -f "$TARGET_JAR" ]; then
  echo "Core server is not included in this Rill distribution."
  exit 2
fi

exec "$JAVA_EXE" -jar "$TARGET_JAR" "$@"