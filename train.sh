#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ "${OS:-}" == "Windows_NT" ]]; then
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/train.ps1 "$@"
  exit $?
fi
if [[ ! -d target/dependency ]]; then BDA_SKIP_TRAINING=1 bash setup.sh; fi
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then JAVA="$JAVA_HOME/bin/java"; else JAVA=java; fi
if [[ -f .tools/java-command ]]; then JAVA="$(cat .tools/java-command)"; fi
opens=()
for package in java.lang java.lang.invoke java.lang.reflect java.io java.net java.nio java.util java.util.concurrent java.util.concurrent.atomic sun.nio.ch sun.nio.cs sun.security.action sun.util.calendar; do
  opens+=("--add-opens=java.base/$package=ALL-UNNAMED")
done
exec "$JAVA" -Xmx2g "${opens[@]}" -cp 'target/classes:target/dependency/*' college.bda.Main train "$@"
