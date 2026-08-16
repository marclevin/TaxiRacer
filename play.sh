#!/usr/bin/env bash
#
# Builds and runs Taxi Racer natively on Linux or macOS.
#
# Needs a JDK 17 or newer on the PATH and nothing else. The JavaFX SDK is downloaded once
# into .javafx/ and reused afterwards. Set JAVAFX_HOME to use an SDK you already have.
#
set -euo pipefail

cd "$(dirname "$0")"

JAVAFX_VERSION="21.0.5"
CLASSES_DIR="build/classes"
# Cached outside the project: the SDK is ~120 MB and this repository may well live in a
# synced folder, where a dependency cache has no business being uploaded to the cloud.
SDK_ROOT="${XDG_CACHE_HOME:-$HOME/.cache}/taxiracer/javafx"

if ! command -v javac >/dev/null 2>&1; then
    echo "No JDK found. Install one (Temurin 21 from https://adoptium.net), or use Docker instead: docker compose up" >&2
    exit 1
fi

javac_version="$(javac -version 2>&1)"
major="$(printf '%s' "${javac_version}" | grep -oE '[0-9]+' | head -1)"
if [ "${major}" -lt 17 ]; then
    echo "JDK 17 or newer is required; found ${javac_version}" >&2
    exit 1
fi
echo "Using ${javac_version}"

# --- Locate or fetch JavaFX -------------------------------------------------
resolve_javafx() {
    if [ -n "${JAVAFX_HOME:-}" ] && [ -d "${JAVAFX_HOME}/lib" ]; then
        echo "${JAVAFX_HOME}/lib"
        return
    fi

    for root in "${SDK_ROOT}" ".javafx"; do
        existing="$(find "${root}" -maxdepth 1 -type d -name 'javafx-sdk-*' 2>/dev/null | head -1 || true)"
        if [ -n "${existing}" ]; then
            echo "${existing}/lib"
            return
        fi
    done

    case "$(uname -s)" in
        Darwin) os="osx" ;;
        Linux)  os="linux" ;;
        *)      echo "Unsupported platform $(uname -s); set JAVAFX_HOME by hand." >&2; exit 1 ;;
    esac
    case "$(uname -m)" in
        arm64|aarch64) arch="aarch64" ;;
        *)             arch="x64" ;;
    esac

    url="https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/openjfx-${JAVAFX_VERSION}_${os}-${arch}_bin-sdk.zip"
    echo "Downloading the JavaFX ${JAVAFX_VERSION} SDK (about 50 MB, once)..." >&2
    mkdir -p "${SDK_ROOT}"
    tmp="$(mktemp -t javafx.XXXXXX.zip)"
    if ! curl -fsSL "${url}" -o "${tmp}"; then
        echo "Could not download JavaFX from ${url}. Download it by hand and set JAVAFX_HOME." >&2
        exit 1
    fi
    unzip -q -o "${tmp}" -d "${SDK_ROOT}"
    rm -f "${tmp}"

    existing="$(find "${SDK_ROOT}" -maxdepth 1 -type d -name 'javafx-sdk-*' | head -1)"
    echo "${existing}/lib"
}

FX_LIB="$(resolve_javafx)"

# --- Build ------------------------------------------------------------------
if [ "${1:-}" = "--clean" ]; then
    rm -rf "${CLASSES_DIR}"
fi

mkdir -p "${CLASSES_DIR}"
if [ -z "$(find src -name '*.java' -newer "${CLASSES_DIR}/Launcher.class" 2>/dev/null)" ] \
   && [ -f "${CLASSES_DIR}/Launcher.class" ]; then
    echo "Sources unchanged; skipping compile."
else
    echo "Compiling..."
    find src -name '*.java' -print0 \
        | xargs -0 javac -d "${CLASSES_DIR}" --module-path "${FX_LIB}" --add-modules javafx.controls,javafx.media
    # Assets travel on the classpath so they resolve the same way they do from the jar.
    cp -r img dat res "${CLASSES_DIR}/"
fi

echo "Starting Taxi Racer..."
exec java --module-path "${FX_LIB}" --add-modules javafx.controls,javafx.media -cp "${CLASSES_DIR}" Launcher
