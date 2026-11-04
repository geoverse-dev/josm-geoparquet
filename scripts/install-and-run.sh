#!/usr/bin/env bash
set -euo pipefail

GEOPARQUET_VERSION="${GEOPARQUET_VERSION:-0.3.0}"
JOSM_URL="${JOSM_URL:-https://josm.openstreetmap.de/download/josm-tested.jar}"
BUNDLE_DIR="${JOSM_GEOPARQUET_DIR:-$PWD/josm-geoparquet-bundle}"
REFRESH=false
PREPARE_ONLY=false

usage() {
  cat <<'EOF'
Usage / uso: scripts/install-and-run.sh [options] [-- JOSM arguments]

Options / opciones:
  --dir DIR        Bundle directory / carpeta del entorno.
  --refresh        Download assets again / vuelve a descargar los archivos.
  --prepare-only   Install and verify without starting JOSM.
                   Instala y verifica sin iniciar JOSM.
  -h, --help       Show this help / muestra esta ayuda.

Environment / variables:
  JAVA_HOME, JOSM_HEAP (default 4g), JOSM_GEOPARQUET_DIR,
  GEOPARQUET_VERSION, JOSM_URL.
EOF
}

JOSM_ARGS=()
while (($#)); do
  case "$1" in
    --dir) [[ $# -ge 2 ]] || { echo 'ERROR: --dir requires a directory.' >&2; exit 2; }; BUNDLE_DIR="$2"; shift 2 ;;
    --refresh) REFRESH=true; shift ;;
    --prepare-only) PREPARE_ONLY=true; shift ;;
    -h|--help) usage; exit 0 ;;
    --) shift; JOSM_ARGS+=("$@"); break ;;
    *) JOSM_ARGS+=("$1"); shift ;;
  esac
done

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "ERROR: missing required command / falta el comando requerido: $1" >&2
    exit 1
  }
}
for command in curl unzip sha256sum awk mktemp sed; do require_command "$command"; done

if [[ -n "${JAVA_HOME:-}" ]]; then JAVA="$JAVA_HOME/bin/java"; else JAVA="$(command -v java || true)"; fi
if [[ ! -x "$JAVA" ]]; then
  echo 'ERROR: Java not found. Install JDK 25 or set JAVA_HOME.' >&2
  echo 'ERROR: Java no encontrado. Instalá JDK 25 o definí JAVA_HOME.' >&2
  exit 1
fi
JAVA_VERSION="$("$JAVA" -version 2>&1 | awk -F '"' '/version/ {print $2; exit}')"
JAVA_MAJOR="${JAVA_VERSION%%.*}"
if [[ "$JAVA_MAJOR" != "25" ]]; then
  echo "ERROR: Java 25 is required / se requiere Java 25 (found/encontrado: ${JAVA_VERSION:-unknown})." >&2
  exit 1
fi

JOSM_JAR="$BUNDLE_DIR/josm-tested.jar"
PREFERENCES_DIR="$BUNDLE_DIR/preferences"
USER_DATA_DIR="$BUNDLE_DIR/user-data"
CACHE_DIR="$BUNDLE_DIR/cache"
PLUGINS_DIR="$USER_DATA_DIR/plugins"
PLUGIN_ASSET="geoparquet-${GEOPARQUET_VERSION}.jar"
mkdir -p "$BUNDLE_DIR" "$PREFERENCES_DIR" "$PLUGINS_DIR" "$CACHE_DIR"

download() {
  local url="$1" destination="$2" temporary
  if [[ "$REFRESH" == false && -s "$destination" ]]; then echo "[cache] $destination"; return; fi
  temporary="$(mktemp "$BUNDLE_DIR/download.XXXXXX")"
  trap 'rm -f -- "$temporary"' RETURN
  echo "[download] $url"
  curl --fail --location --retry 3 --connect-timeout 20 --output "$temporary" "$url"
  mv -- "$temporary" "$destination"
  trap - RETURN
}

download "$JOSM_URL" "$JOSM_JAR"
unzip -tqq "$JOSM_JAR" >/dev/null 2>&1 || { echo "ERROR: invalid JOSM JAR: $JOSM_JAR" >&2; exit 1; }
JOSM_VERSION="$("$JAVA" -jar "$JOSM_JAR" --version | sed -n 's/.*(\([0-9][0-9]*\).*/\1/p')"
[[ -n "$JOSM_VERSION" ]] || { echo 'ERROR: could not determine JOSM version.' >&2; exit 1; }

RELEASE_URL="https://github.com/geoverse-dev/josm-geoparquet/releases/download/v$GEOPARQUET_VERSION"
download "$RELEASE_URL/$PLUGIN_ASSET" "$PLUGINS_DIR/geoparquet.jar"
download "$RELEASE_URL/$PLUGIN_ASSET.sha256" "$PLUGINS_DIR/$PLUGIN_ASSET.sha256"
(
  cd "$PLUGINS_DIR"
  cp -- geoparquet.jar "$PLUGIN_ASSET"
  sha256sum -c "$PLUGIN_ASSET.sha256"
  rm -f -- "$PLUGIN_ASSET"
)

if [[ ! -e "$PREFERENCES_DIR/preferences.xml" ]]; then
  cat > "$PREFERENCES_DIR/preferences.xml" <<XML
<?xml version="1.0" encoding="UTF-8"?>
<preferences xmlns="http://josm.openstreetmap.de/preferences-1.0" version="$JOSM_VERSION">
  <tag key="josm.version" value="$JOSM_VERSION"/>
  <tag key="pluginmanager.version" value="$JOSM_VERSION"/>
  <list key="plugins"><entry value="geoparquet"/></list>
</preferences>
XML
fi

echo "[ready] Java $JAVA_VERSION"
echo "[ready] JOSM $JOSM_VERSION"
echo "[ready] GeoParquet $GEOPARQUET_VERSION"
echo "[ready] Bundle / entorno: $BUNDLE_DIR"
if [[ "$PREPARE_ONLY" == true ]]; then exit 0; fi

exec "$JAVA" --enable-preview \
  --add-exports=java.base/sun.security.action=ALL-UNNAMED \
  --add-exports=java.desktop/com.sun.imageio.plugins.jpeg=ALL-UNNAMED \
  --add-exports=java.desktop/com.sun.imageio.spi=ALL-UNNAMED \
  -Xmx"${JOSM_HEAP:-4g}" \
  -Djosm.pref="$PREFERENCES_DIR" -Djosm.userdata="$USER_DATA_DIR" -Djosm.cache="$CACHE_DIR" \
  -jar "$JOSM_JAR" "${JOSM_ARGS[@]}"
