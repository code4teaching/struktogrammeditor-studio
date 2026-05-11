#!/usr/bin/env bash
# Schlankes .app: nur Launcher + JAR, Java kommt vom System (java_home / PATH, 17+).
# Vorher: ./mvnw -q clean package
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
VER="${PROJECT_VERSION:-1.1.3}"
JAR_NAME="visustruct-${VER}.jar"
OUT="${ROOT}/dist/macos-app-light"
APP="${OUT}/VisuStruct.app"

if [[ ! -f "${ROOT}/target/${JAR_NAME}" ]]; then
	echo "Fehlt: ${ROOT}/target/${JAR_NAME} — zuerst ./mvnw -q clean package" >&2
	exit 1
fi

rm -rf "${APP}"
mkdir -p "${APP}/Contents/MacOS" "${APP}/Contents/Java"

sed "s/__VERSION__/${VER}/g" "${SCRIPT_DIR}/lightweight/Info.plist.in" > "${APP}/Contents/Info.plist"
sed "s/__JAR_NAME__/${JAR_NAME}/g" "${SCRIPT_DIR}/lightweight/launcher.in" > "${APP}/Contents/MacOS/launcher"
chmod +x "${APP}/Contents/MacOS/launcher"
cp "${ROOT}/target/${JAR_NAME}" "${APP}/Contents/Java/"

echo "Schlanke App VisuStruct.app (ohne eingebettete JRE): ${APP}"
du -sh "${APP}" 2>/dev/null || true
