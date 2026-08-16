#!/usr/bin/env bash
#
# Runs Taxi Racer in Docker and opens it in your browser.
#
# The one-command way to play. Needs Docker and nothing else - no Java, no JavaFX, no
# build tools. Press Ctrl+C to stop the game and shut the container down.
#
set -euo pipefail

cd "$(dirname "$0")"

URL="http://localhost:8080/"

if ! command -v docker >/dev/null 2>&1; then
    echo "Docker is not installed. Install it from https://docker.com, or run natively with ./play.sh (needs a JDK)." >&2
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "Docker is installed but the engine is not running. Start Docker and try again." >&2
    exit 1
fi

REBUILD=""
if [ "${1:-}" = "--rebuild" ]; then
    REBUILD="--build"
fi

echo "Building and starting Taxi Racer (the first run downloads and compiles, so give it a minute)..."
docker compose up -d ${REBUILD}

echo "Waiting for the game to come up..."
ready=""
for _ in $(seq 1 60); do
    if curl -fsS -o /dev/null "${URL}" 2>/dev/null; then
        ready="yes"
        break
    fi
    sleep 1
done

if [ -z "${ready}" ]; then
    echo "The game did not answer on ${URL} in time. Recent container output:" >&2
    docker compose logs --tail 40
    exit 1
fi

cat <<EOF

  Taxi Racer is running at ${URL}
  Saves are kept in ./saves
  Press Ctrl+C to stop.

EOF

# Open a browser if the platform gives us a way to.
if command -v xdg-open >/dev/null 2>&1; then xdg-open "${URL}" >/dev/null 2>&1 || true
elif command -v open    >/dev/null 2>&1; then open "${URL}"     >/dev/null 2>&1 || true
fi

trap 'echo; echo "Stopping Taxi Racer..."; docker compose down >/dev/null 2>&1 || true' EXIT
docker compose logs -f
