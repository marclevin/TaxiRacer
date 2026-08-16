#!/bin/sh
#
# Starts a throwaway X display, publishes it over the browser, and runs the game on it.
#
# The game is a desktop JavaFX application, so the container carries its own display:
# Xvfb provides one in memory, x11vnc exports it, and noVNC serves a VNC client over
# plain HTTP so that playing needs nothing but a browser.
#
set -eu

SCREEN_W="${SCREEN_W:-1120}"
SCREEN_H="${SCREEN_H:-800}"
VNC_PORT="${VNC_PORT:-5900}"
WEB_PORT="${WEB_PORT:-8080}"
DISPLAY_NUM="${DISPLAY_NUM:-0}"

export DISPLAY=":${DISPLAY_NUM}"

cleanup() {
    # Take the whole container down with the game, rather than leaving an empty desktop
    # serving on port 8080 after the player picks Exit.
    kill 0 2>/dev/null || true
}
trap cleanup EXIT INT TERM

Xvfb "${DISPLAY}" -screen 0 "${SCREEN_W}x${SCREEN_H}x24" -nolisten tcp &

# Wait for the display to accept connections before starting anything that needs it.
i=0
until xdpyinfo -display "${DISPLAY}" >/dev/null 2>&1; do
    i=$((i + 1))
    if [ "${i}" -gt 200 ]; then
        echo "Xvfb did not come up on ${DISPLAY}" >&2
        exit 1
    fi
    sleep 0.1
done

# A minimal window manager. Without one the game window cannot be moved or focused,
# and modal dialogs open without decoration.
fluxbox >/dev/null 2>&1 &

x11vnc -display "${DISPLAY}" -forever -shared -nopw -quiet -noxdamage \
       -rfbport "${VNC_PORT}" >/dev/null 2>&1 &

websockify --web=/usr/share/novnc "${WEB_PORT}" "localhost:${VNC_PORT}" >/dev/null 2>&1 &

cat <<BANNER

  ============================================================
    Taxi Racer is running.

    Open  http://localhost:${WEB_PORT}/  in your browser.

    Saves are kept in ./saves on the host.
    Press Ctrl+C here, or Exit in the game, to stop.
  ============================================================

BANNER

# Software rendering: there is no GPU in the container, and prism's default pipeline
# would fail over noisily before falling back on its own.
exec java \
    -Dprism.order=sw \
    -Dprism.lcdtext=false \
    -Djava.awt.headless=false \
    -Dtaxiracer.saveDir=/app/saves \
    -jar /app/taxiracer.jar
