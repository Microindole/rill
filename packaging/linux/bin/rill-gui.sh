#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
APP_HOME=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

exec "$APP_HOME/runtime/bin/java" -jar "$APP_HOME/client/rill-gui.jar" "$@"
