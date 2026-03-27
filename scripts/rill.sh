#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
if [ -n "${JAVA21_HOME:-}" ]; then
  export JAVA_HOME="$JAVA21_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

MODE="${1:-}"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/rill.sh <mode> [args]

Modes:
  server        Start the native rill TCP server
  mysql-server  Start the MySQL protocol server
  client        Start the terminal client
  gui           Start the Swing GUI client
  spring        Start the Spring Boot Web application

Examples:
  ./scripts/rill.sh server --port=8848
  ./scripts/rill.sh client --host=127.0.0.1 --port=8848 --user=root
  ./scripts/rill.sh spring
EOF
}

case "$MODE" in
  ""|help|-h|--help)
    usage
    exit 0
    ;;
  server)
    TARGET_DIR="$SCRIPT_DIR/../rill-server/target"
    ARTIFACT_PATTERN='rill-server-*-server.jar'
    ;;
  mysql-server)
    TARGET_DIR="$SCRIPT_DIR/../rill-server/target"
    ARTIFACT_PATTERN='rill-server-*-mysql-server.jar'
    ;;
  client)
    TARGET_DIR="$SCRIPT_DIR/../rill-client/target"
    ARTIFACT_PATTERN='rill-client-*-cli.jar'
    ;;
  gui)
    TARGET_DIR="$SCRIPT_DIR/../rill-client/target"
    ARTIFACT_PATTERN='rill-client-*-gui.jar'
    ;;
  spring)
    TARGET_DIR="$SCRIPT_DIR/../rill-app-web/target"
    ARTIFACT_PATTERN='rill-app-web-*.jar'
    ;;
  *)
    echo "Unsupported mode: $MODE"
    echo "Supported modes: server, mysql-server, client, gui, spring"
    exit 1
    ;;
esac

shift

JAR_PATH=$(ls -1t "$TARGET_DIR"/$ARTIFACT_PATTERN 2>/dev/null | head -n 1 || true)

if [ -z "$JAR_PATH" ]; then
  echo "No packaged $MODE jar found."
  echo "Run ./scripts/build.sh first."
  exit 1
fi

if [ -n "${JAVA_HOME:-}" ]; then
  exec "$JAVA_HOME/bin/java" -jar "$JAR_PATH" "$@"
fi

exec java -jar "$JAR_PATH" "$@"
