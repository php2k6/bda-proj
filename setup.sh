#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ "${OS:-}" == "Windows_NT" ]]; then
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/setup.ps1
  exit $?
fi
major() { java -version 2>&1 | head -n1 | sed -E 's/.*version "([0-9]+).*/\1/'; }
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
if ! command -v java >/dev/null 2>&1 || ! command -v javac >/dev/null 2>&1 || [[ "$(major)" != 17 ]]; then
  if [[ "$(uname)" == Darwin ]] && command -v brew >/dev/null; then
    brew install openjdk@17
    export JAVA_HOME="$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home"
    export PATH="$JAVA_HOME/bin:$PATH"
  elif command -v apt-get >/dev/null; then
    if [[ "$EUID" == 0 ]]; then SUDO=(); else SUDO=(sudo); fi
    "${SUDO[@]}" apt-get update
    "${SUDO[@]}" apt-get install -y openjdk-17-jdk curl unzip
    export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-$(uname -m | sed 's/x86_64/amd64/;s/aarch64/arm64/')"
    export PATH="$JAVA_HOME/bin:$PATH"
  else
    echo 'Install JDK 17 for your platform, set JAVA_HOME, then run setup.sh again.' >&2; exit 1
  fi
fi
if ! command -v curl >/dev/null || ! command -v unzip >/dev/null; then
  if command -v apt-get >/dev/null; then
    if [[ "$EUID" == 0 ]]; then SUDO=(); else SUDO=(sudo); fi
    "${SUDO[@]}" apt-get update
    "${SUDO[@]}" apt-get install -y curl unzip
  else
    echo 'Install curl and unzip, then rerun setup.sh.' >&2; exit 1
  fi
fi
mkdir -p .tools
if [[ -n "${JAVA_HOME:-}" ]]; then printf '%s\n' "$JAVA_HOME/bin/java" > .tools/java-command; else command -v java > .tools/java-command; fi
MAVEN=.tools/apache-maven-3.9.11/bin/mvn
if [[ ! -x "$MAVEN" ]]; then
  curl --fail --location --retry 2 https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip -o .tools/maven.zip
  expected=0d7125e8c91097b36edb990ea5934e6c68b4440eef4ea96510a0f6815e7eeadb
  if command -v sha256sum >/dev/null; then actual=$(sha256sum .tools/maven.zip | cut -d' ' -f1); else actual=$(shasum -a 256 .tools/maven.zip | cut -d' ' -f1); fi
  [[ "$actual" == "$expected" ]] || { echo 'Maven checksum mismatch.' >&2; exit 1; }
  unzip -q -o .tools/maven.zip -d .tools
  chmod +x "$MAVEN"
fi
"$MAVEN" -B -ntp package dependency:copy-dependencies -DincludeScope=compile -DoutputDirectory=target/dependency
mkdir -p dist
cp target/movie-recommender.jar dist/movie-recommender.jar
if [[ ! -f data/ml-latest-small/ratings.csv ]]; then java -jar dist/movie-recommender.jar download; fi
if [[ "${BDA_SKIP_TRAINING:-0}" != 1 && ! -f artifacts/model.json ]]; then bash train.sh; fi
echo 'Setup complete. Run: bash run.sh'
