#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
if [ -n "${JAVA21_HOME:-}" ]; then
  export JAVA_HOME="$JAVA21_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

exec "$SCRIPT_DIR/../mvnw" -DskipTests package "$@"
