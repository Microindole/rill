#!/usr/bin/env sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
JAVA_EXE="$APP_HOME/runtime/bin/java"
LAUNCHER_JAR="$APP_HOME/rill-launcher.jar"
MODE="${1:-}"

usage() {
  cat <<'EOF'
Usage:
  rill <mode> [args]

Modes:
  server        Start the native rill TCP server
  mysql-server  Start the MySQL protocol server
  sql           Start the terminal SQL client
  gui           Start the Swing GUI client
  log           Inspect log files
  data          Inspect or export database files

Compatibility aliases:
  client        Alias for sql
  log-reader    Alias for log
  data-reader   Alias for data
EOF
}

ensure_tool() {
  if [ ! -x "$JAVA_EXE" ]; then
    echo "Missing bundled runtime: $JAVA_EXE"
    exit 1
  fi
  if [ ! -f "$LAUNCHER_JAR" ]; then
    echo "$1 is not included in this Rill distribution."
    exit 2
  fi
}

case "$MODE" in
  ""|help|-h|--help)
    usage
    ;;
  server)
    shift
    exec "$APP_HOME/bin/rill-server.sh" "$@"
    ;;
  mysql-server)
    shift
    exec "$APP_HOME/bin/rill-mysql.sh" "$@"
    ;;
  sql|client)
    shift
    exec "$APP_HOME/bin/rill-cli.sh" "$@"
    ;;
  gui)
    shift
    exec "$APP_HOME/bin/rill-gui.sh" "$@"
    ;;
  log|log-reader)
    shift
    ensure_tool "Log tool"
    exec "$JAVA_EXE" -jar "$LAUNCHER_JAR" log "$@"
    ;;
  data|data-reader)
    shift
    ensure_tool "Data tool"
    exec "$JAVA_EXE" -jar "$LAUNCHER_JAR" data "$@"
    ;;
  *)
    echo "Unsupported mode: $MODE"
    usage
    exit 1
    ;;
esac