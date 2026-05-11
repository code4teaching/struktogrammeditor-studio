#!/usr/bin/env bash
# Erzeugt ein .app mit eingebetteter Java-Runtime (jpackage) — oft ~150 MB+.
# Schlankes Bundle ohne JRE: ./build-lightweight-app.sh (nur JAR + System-Java).
# Voraussetzung: JDK 17+ mit jpackage (im PATH oder JAVA_HOME).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
VER="${PROJECT_VERSION:-1.1.3}"
JAR="visustruct-${VER}.jar"
INPUT="${ROOT}/target"
OUT="${ROOT}/dist/macos-app"

if [[ ! -f "${INPUT}/${JAR}" ]]; then
	echo "Fehlt: ${INPUT}/${JAR} — bitte zuerst im Projektroot ./mvnw -q clean package ausführen." >&2
	exit 1
fi

if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/jpackage" ]]; then
	JPACKAGE="${JAVA_HOME}/bin/jpackage"
elif command -v jpackage >/dev/null 2>&1; then
	JPACKAGE="jpackage"
else
	echo "jpackage nicht gefunden. JDK 17+ installieren und JAVA_HOME setzen." >&2
	exit 1
fi

rm -rf "${OUT}"
mkdir -p "${OUT}"

"$JPACKAGE" \
	--type app-image \
	--input "${INPUT}" \
	--main-jar "${JAR}" \
	--main-class de.visustruct.control.Main \
	--name "VisuStruct" \
	--app-version "${VER}" \
	--dest "${OUT}" \
	--file-associations "${ROOT}/packaging/macos/file-association-visustruct.properties"

echo "App-Bundle: ${OUT}/VisuStruct.app"
echo "Diese App in /Applications legen oder per Doppelklick starten; .visustruct, .xml, ggf. ältere .strk mit „Öffnen mit“ zuordnen."
