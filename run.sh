#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ "${OS:-}" == "Windows_NT" ]]; then powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/run.ps1 "$@"; exit $?; fi
if ! command -v java >/dev/null; then bash setup.sh; fi
if [[ ! -f dist/movie-recommender.jar || ! -f artifacts/model.json ]]; then bash setup.sh; fi
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then JAVA="$JAVA_HOME/bin/java"; else JAVA=java; fi
if [[ -f .tools/java-command ]]; then JAVA="$(cat .tools/java-command)"; fi
echo 'Open http://127.0.0.1:8080 in your browser. Press Ctrl+C to stop.'
exec "$JAVA" -jar dist/movie-recommender.jar serve "$@"
