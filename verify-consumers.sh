#!/bin/sh
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$root"
mode=${1:-candidate}
version=${2:-1.3.2}
case "$mode" in candidate|public) ;; *) echo 'Usage: verify-consumers.sh [candidate|public] [version]' >&2; exit 2;; esac
results="$root/build/consumer-results/$mode"
mkdir -p "$results"
java_runtime="${TEST_JAVA_HOME:-$JAVA_HOME}/bin/java"
repository="$root/build/repository"
if [ "$mode" = candidate ]; then
    sh ./gradlew --no-daemon --console=plain check publishMavenJavaPublicationToCandidateRepository > "$results/library.log" 2>&1
    python3 .github/scripts/check-documentation.py > "$results/documentation.log"
    python3 .github/scripts/check-compatibility.py "$repository/io/github/lambig/Patterns/$version/Patterns-$version.jar" > "$results/compatibility.log" 2>&1
fi
cache=$(mktemp -d)
trap 'rm -rf "$cache"' EXIT HUP INT TERM
if [ "$mode" = candidate ]; then set -- "-PcandidateRepository=$repository"; else set --; fi
GRADLE_USER_HOME="$cache/gradle" sh ./gradlew -p example --no-daemon --console=plain \
    "-PlibraryVersion=$version" "$@" clean runExample > "$results/gradle.log" 2>&1
if [ "$mode" = candidate ]; then set -- "-DcandidateRepository=file://$repository"; else set --; fi
mvn -B -ntp -f example/pom.xml "-Dmaven.repo.local=$cache/maven" "-DlibraryVersion=$version" "$@" \
    clean compile dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt > "$results/maven.log" 2>&1
classpath=$(cat example/target/runtime-classpath.txt)
python3 - "$classpath" "$version" <<'PY'
import os, sys
from pathlib import Path
assert sorted(Path(p).name for p in sys.argv[1].split(os.pathsep)) == sorted([
    'Patterns-' + sys.argv[2] + '.jar', 'Tuplite-1.0.1.jar', 'functional-interface-extension-1.1.3.jar'])
PY
"$java_runtime" -cp "example/target/classes:$classpath" example.PatternExample >> "$results/maven.log" 2>&1
python3 .github/scripts/check-readme-example.py "$classpath" > "$results/readme.log" 2>&1
grep -F 'classification=large; fallback=unknown; events=[large:12, other:0]; tuple-api=OK' "$results/gradle.log" "$results/maven.log"
echo "Consumer verification passed: $mode $version"
