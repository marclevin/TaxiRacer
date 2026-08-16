package game.display.view;

import java.util.Random;

import game.utility.ESettings;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * The scenery behind the road: sky, sun, clouds and a city skyline.
 *
 * <p>
 * Each layer scrolls at its own fraction of the road speed. That parallax is what sells the
 * sense of travelling somewhere — without it the road slides past a static picture and the
 * taxi reads as running on a treadmill.
 * </p>
 */
final class Backdrop {

    private static final double WIDTH = ESettings.SCENE_WIDTH.getVal();
    private static final double HEIGHT = ESettings.SCENE_HEIGHT.getVal();
    private static final double HORIZON = ESettings.ROAD_Y.getVal();

    /** How much of the road's speed each layer travels at. */
    private static final double CLOUD_PARALLAX = 0.06;
    private static final double SKYLINE_PARALLAX = 0.18;
    private static final double HEDGE_PARALLAX = 0.42;

    /** Layers repeat over this span before wrapping. */
    private static final double TILE = WIDTH * 2;

    private final LinearGradient sky;
    private final Building[] skyline;
    private final Cloud[] clouds;
    private final double[] hedge;

    private double cloudOffset = 0;
    private double skylineOffset = 0;
    private double hedgeOffset = 0;
    private double vergeOffset = 0;

    /**
     * Builds a fixed city. The layout is generated from a constant seed so that the skyline
     * is the same every run rather than reshuffling each time the player restarts.
     */
    Backdrop() {
        sky = new LinearGradient(0, 0, 0, HORIZON, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.web("#1e4c8a")),
                new Stop(0.45, Color.web("#5aa0d0")),
                new Stop(0.80, Color.web("#a8cfe4")),
                new Stop(1.00, Color.web("#e6d9b8")));

        Random rng = new Random(20220523L);

        int count = 46;
        skyline = new Building[count];
        double x = -WIDTH * 0.5;
        for (int i = 0; i < count; i++) {
            Building b = new Building();
            b.x = x;
            b.width = 34 + rng.nextInt(62);
            b.height = 40 + rng.nextInt(135);
            b.shade = 0.22 + rng.nextDouble() * 0.16;
            b.windowSeed = rng.nextLong();
            skyline[i] = b;
            x += b.width + 4 + rng.nextInt(22);
        }

        clouds = new Cloud[9];
        for (int i = 0; i < clouds.length; i++) {
            Cloud c = new Cloud();
            c.x = rng.nextDouble() * TILE;
            c.y = 24 + rng.nextDouble() * (HORIZON * 0.45);
            c.scale = 0.6 + rng.nextDouble() * 1.1;
            clouds[i] = c;
        }

        hedge = new double[120];
        for (int i = 0; i < hedge.length; i++) {
            hedge[i] = 10 + rng.nextDouble() * 16;
        }
    }

    /**
     * Scrolls every layer.
     *
     * @param speed the road's scroll speed for this step, in pixels (negative moves left)
     */
    void update(double speed) {
        cloudOffset = wrap(cloudOffset + speed * CLOUD_PARALLAX);
        skylineOffset = wrap(skylineOffset + speed * SKYLINE_PARALLAX);
        hedgeOffset = wrap(hedgeOffset + speed * HEDGE_PARALLAX);
        // The nearest layer travels at full road speed, which is what makes the sense of
        // speed read at the bottom of the screen where the eye actually is.
        vergeOffset = wrap(vergeOffset + speed);
    }

    /**
     * Resets the scenery to its starting position.
     */
    void reset() {
        cloudOffset = 0;
        skylineOffset = 0;
        hedgeOffset = 0;
        vergeOffset = 0;
    }

    /**
     * Draws the near verge, on top of the road layer so its tufts overlap the kerb.
     *
     * @param gc       the graphics context
     * @param vergeTop the Y coordinate where the road sprite ends
     */
    void renderForeground(GraphicsContext gc, double vergeTop) {
        gc.setFill(Color.web("#86ab63"));
        gc.fillRect(0, vergeTop, WIDTH, HEIGHT - vergeTop);

        // Tufts of grass streaking past at full speed.
        gc.setFill(Color.web("#6d9150"));
        for (int pass = 0; pass < 2; pass++) {
            double shift = vergeOffset + pass * TILE;
            for (int i = 0; i < hedge.length; i++) {
                double vx = i * 19 - WIDTH * 0.5 + shift;
                if (vx < -40 || vx > WIDTH + 40) {
                    continue;
                }
                double h = 5 + (hedge[i] * 0.5);
                gc.fillOval(vx, vergeTop + 14 + (i % 3) * 17, 26, h);
            }
        }

        // A little depth at the very bottom of the frame.
        gc.setFill(Color.color(0, 0, 0, 0.13));
        gc.fillRect(0, HEIGHT - 26, WIDTH, 26);
    }

    /**
     * Draws every layer behind the road.
     *
     * @param gc the graphics context
     */
    void render(GraphicsContext gc) {
        gc.setFill(sky);
        gc.fillRect(0, 0, WIDTH, HORIZON);

        drawSun(gc);
        drawClouds(gc);
        drawSkyline(gc);
        drawHedge(gc);

        // The near verge, below the road surface.
        gc.setFill(Color.web("#7fa05c"));
        gc.fillRect(0, HORIZON, WIDTH, HEIGHT - HORIZON);
        gc.setFill(Color.web("#8fb268"));
        gc.fillRect(0, HORIZON + 250, WIDTH, HEIGHT - HORIZON - 250);
    }

    /**
     * Draws a hazy low sun.
     *
     * <p>
     * The glow is a radial gradient rather than a flat translucent disc, which would read as
     * a hard-edged bubble stuck on the sky.
     * </p>
     *
     * @param gc the graphics context
     */
    private void drawSun(GraphicsContext gc) {
        double cx = WIDTH * 0.72;
        double cy = HORIZON * 0.30;

        gc.setFill(new RadialGradient(0, 0, cx, cy, 110, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.color(1.0, 0.97, 0.80, 0.55)),
                new Stop(0.35, Color.color(1.0, 0.94, 0.74, 0.22)),
                new Stop(1.00, Color.color(1.0, 0.92, 0.70, 0.0))));
        gc.fillOval(cx - 110, cy - 110, 220, 220);

        gc.setFill(Color.color(1.0, 0.97, 0.84, 0.92));
        gc.fillOval(cx - 32, cy - 32, 64, 64);
    }

    /**
     * Draws the cloud layer twice so it wraps without a seam.
     *
     * @param gc the graphics context
     */
    private void drawClouds(GraphicsContext gc) {
        gc.setFill(Color.color(1, 1, 1, 0.72));
        for (int pass = 0; pass < 2; pass++) {
            double shift = cloudOffset + pass * TILE;
            for (Cloud c : clouds) {
                double cx = c.x + shift;
                if (cx < -260 || cx > WIDTH + 60) {
                    continue;
                }
                double w = 70 * c.scale;
                double h = 22 * c.scale;
                gc.fillOval(cx, c.y, w, h);
                gc.fillOval(cx + w * 0.32, c.y - h * 0.45, w * 0.82, h * 1.25);
                gc.fillOval(cx + w * 0.72, c.y + h * 0.06, w * 0.68, h * 0.92);
            }
        }
    }

    /**
     * Draws the city skyline sitting on the horizon.
     *
     * @param gc the graphics context
     */
    private void drawSkyline(GraphicsContext gc) {
        for (int pass = 0; pass < 2; pass++) {
            double shift = skylineOffset + pass * TILE;
            for (Building b : skyline) {
                double bx = b.x + shift;
                if (bx + b.width < -20 || bx > WIDTH + 20) {
                    continue;
                }
                double by = HORIZON - b.height;

                gc.setFill(Color.color(b.shade, b.shade + 0.05, b.shade + 0.14, 0.92));
                gc.fillRect(bx, by, b.width, b.height);

                // Lit windows, laid out from the building's own seed so they stay put.
                gc.setFill(Color.color(1.0, 0.90, 0.62, 0.5));
                long seed = b.windowSeed;
                for (double wy = by + 8; wy < HORIZON - 8; wy += 11) {
                    for (double wx = bx + 6; wx < bx + b.width - 8; wx += 10) {
                        seed = seed * 6364136223846793005L + 1442695040888963407L;
                        if ((seed >>> 60) % 5 < 2) {
                            gc.fillRect(wx, wy, 4, 5);
                        }
                    }
                }
            }
        }
    }

    /**
     * Draws the scrubby hedge line just behind the road.
     *
     * @param gc the graphics context
     */
    private void drawHedge(GraphicsContext gc) {
        gc.setFill(Color.web("#3f6b3a"));
        for (int pass = 0; pass < 2; pass++) {
            double shift = hedgeOffset + pass * TILE;
            for (int i = 0; i < hedge.length; i++) {
                double hx = i * 18 - WIDTH * 0.5 + shift;
                if (hx < -30 || hx > WIDTH + 30) {
                    continue;
                }
                gc.fillOval(hx, HORIZON - hedge[i], 30, hedge[i] * 2);
            }
        }
    }

    /**
     * Keeps a layer offset inside one tile so the two drawing passes always cover the screen.
     *
     * @param value the raw offset
     * @return the offset wrapped into {@code (-TILE, 0]}.
     */
    private static double wrap(double value) {
        double wrapped = value;
        while (wrapped <= -TILE) {
            wrapped += TILE;
        }
        while (wrapped > 0) {
            wrapped -= TILE;
        }
        return wrapped;
    }

    /** One silhouette in the skyline. */
    private static final class Building {
        double x;
        double width;
        double height;
        double shade;
        long windowSeed;
    }

    /** One cloud. */
    private static final class Cloud {
        double x;
        double y;
        double scale;
    }
}
