#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
if [ -n "${JAVA21_HOME:-}" ]; then
  export JAVA_HOME="$JAVA21_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

MODE="${1:-}"
FORWARDED_MODE=""

usage() {
  cat <<'EOF'
Usage:
  ./scripts/rill.sh <mode> [args]

Modes:
  server        Start the native rill TCP server
  mysql-server  Start the MySQL protocol server
  sql           Start the terminal SQL client
  gui           Start the Swing GUI client
  web           Start the Spring Boot Web application
  data          Inspect or export database files
  log           Inspect log files

Compatibility aliases:
  client        Alias for sql
  spring        Alias for web
  data-reader   Alias for data
  log-reader    Alias for log

Examples:
  ./scripts/rill.sh server --port=8848
  ./scripts/rill.sh sql --host=127.0.0.1 --port=8848 --user=root
  ./scripts/rill.sh log
  ./scripts/rill.sh web
EOF
}

resolve_jar() {
  mode="$1"
  case "$mode" in
    server)
      find "$SCRIPT_DIR/../rill-server/target" -maxdepth 1 -type f -name 'rill-server-*.jar' \
        ! -name '*-mysql-server.jar' ! -name 'original-*' | sort | tail -n 1
      ;;
    mysql-server)
      find "$SCRIPT_DIR/../rill-server/target" -maxdepth 1 -type f -name 'rill-server-*-mysql-server.jar' \
        ! -name 'original-*' | sort | tail -n 1
      ;;
    sql|client)
      find "$SCRIPT_DIR/../rill-client/target" -maxdepth 1 -type f -name 'rill-client-*-cli.jar' \
        ! -name 'original-*' | sort | tail -n 1
      ;;
    gui)
      find "$SCRIPT_DIR/../rill-client/target" -maxdepth 1 -type f -name 'rill-client-*-gui.jar' \
        ! -name 'original-*' | sort | tail -n 1
      ;;
    web|spring)
      find "$SCRIPT_DIR/../rill-app-web/target" -maxdepth 1 -type f -name 'rill-app-web-*.jar' \
        ! -name '*.jar.original' ! -name 'original-*' | sort | tail -n 1
      ;;
    log|log-reader|data|data-reader)
      find "$SCRIPT_DIR/../rill-launcher/target" -maxdepth 1 -type f -name 'rill-launcher-*.jar' \
        ! -name 'original-*' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | sort | tail -n 1
      ;;
  esac
}

case "$MODE" in
  ""|help|-h|--help)
    usage
    exit 0
    ;;
  log|log-reader)
    FORWARDED_MODE='log'
    ;;
  data|data-reader)
    FORWARDED_MODE='data'
    ;;
  server|mysql-server|sql|client|gui|web|spring)
    ;;
  *)
    echo "Unsupported mode: $MODE"
    echo "Supported modes: server, mysql-server, sql, gui, web, log, data"
    exit 1
    ;;
esac

shift

JAR_PATH="$(resolve_jar "$MODE")"

if [ -z "$JAR_PATH" ]; then
  echo "No packaged $MODE jar found."
  echo "Run ./scripts/build.sh first."
  exit 1
fi

if [ -n "$FORWARDED_MODE" ]; then
  set -- "$FORWARDED_MODE" "$@"
fi

if [ -n "${JAVA_HOME:-}" ]; then
  exec "$JAVA_HOME/bin/java" -jar "$JAR_PATH" "$@"
fi

exec java -jar "$JAR_PATH" "$@"