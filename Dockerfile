# syntax=docker/dockerfile:1
#
# Taxi Racer, packaged so it can be played with one command and no Java installed.
#
# The game is a JavaFX desktop application, so the image ships a display of its own and
# serves it to the browser over noVNC. Stage one builds a self-contained jar with the
# Linux JavaFX natives; stage two is a JRE plus the handful of X libraries JavaFX needs.

# ---------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Dependencies first, so editing game code does not re-download JavaFX on every build.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -Djavafx.platform=linux dependency:go-offline

COPY src ./src
COPY img ./img
COPY dat ./dat
COPY res ./res

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -Djavafx.platform=linux package

# ---------------------------------------------------------------------------
# Runtime
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

# xvfb/x11vnc/novnc provide and publish the display; the lib* packages are what JavaFX
# links against; fonts-dejavu keeps the menus from rendering as boxes.
RUN apt-get update && apt-get install -y --no-install-recommends \
        xvfb \
        x11vnc \
        x11-utils \
        fluxbox \
        novnc \
        websockify \
        libgtk-3-0 \
        libgl1 \
        libxtst6 \
        libxrender1 \
        libasound2 \
        fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

# Land visitors straight in the game rather than on noVNC's connection form.
RUN printf '%s\n' \
    '<!doctype html><meta charset="utf-8"><title>Taxi Racer</title>' \
    '<meta http-equiv="refresh" content="0; url=vnc.html?autoconnect=true&reconnect=true&resize=scale">' \
    '<p style="font-family:sans-serif">Starting Taxi Racer&hellip; <a href="vnc.html?autoconnect=true&resize=scale">continue</a></p>' \
    > /usr/share/novnc/index.html

WORKDIR /app

COPY --from=build /build/target/taxiracer.jar /app/taxiracer.jar
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh && mkdir -p /app/saves

EXPOSE 8080

ENTRYPOINT ["/app/entrypoint.sh"]
