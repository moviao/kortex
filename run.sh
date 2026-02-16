#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Check Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java 21+ is required. Install it from https://adoptium.net"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VER" -lt 21 ]; then
    echo "ERROR: Java 21+ required, found Java $JAVA_VER"
    exit 1
fi

echo "Starting Kortex..."
exec java \
    -Xms64m -Xmx256m \
    -Dllamacpp.base-url="${LLAMACPP_BASE_URL:-http://localhost:8081}" \
    -Dllamacpp.timeout="${LLAMACPP_TIMEOUT:-120}" \
    -Dllamacpp.models-dir="${MODELS_DIR:-$SCRIPT_DIR/models}" \
    -jar "$SCRIPT_DIR/kortex-1.0.0-runner.jar"