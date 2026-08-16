package game.display.view;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import game.utility.ESettings;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Short-lived visual feedback: debris, stains, floating numbers, fares climbing in and out,
 * gunfire and screen shake.
 *
 * <p>
 * These are deliberately kept out of the sprite list. Nothing here can be collided with or
 * scored, it exists purely so that hitting something feels like hitting something.
 * </p>
 *
 * <p>
 * Effects are drawn in three passes because they do not all belong at the same depth. Stains
 * lie on the tarmac and have to go under the traffic; debris and boarding fares sit in among
 * it; the impact flash is a property of the camera and goes over everything, including the
 * screen shake.
 * </p>
 */
final class Effects {

    /** Gravity applied to debris, in pixels per step squared. */
    private static final double GRAVITY = 0.42;
    /** How quickly a shake settles; each step keeps this much of the previous magnitude. */
    private static final double SHAKE_DECAY = 0.88;
    /** Below this magnitude the shake is considered finished. */
    private static final double SHAKE_FLOOR = 0.35;
    /** How long a fare takes to climb in or out, in steps. */
    private static final int TRANSFER_STEPS = 26;
    /** How long a bullet streak is drawn for. */
    private static final int TRACER_STEPS = 6;
    /** How long a mark stays on the tarmac; most scroll off screen long before this. */
    private static final int DECAL_STEPS = 320;
    /** Steps a mark spends fading, at the end of its life. */
    private static final int DECAL_FADE_STEPS = 90;

    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatingText> texts = new ArrayList<>();
    private final List<Transfer> transfers = new ArrayList<>();
    private final List<Decal> decals = new ArrayList<>();
    private final List<Tracer> tracers = new ArrayList<>();

    private double shakeMagnitude = 0;
    private double shakeX = 0;
    private double shakeY = 0;

    private Color flashColour = null;
    private int flashLife = 0;
    private int flashMaxLife = 0;

    private final Font floatFont = Font.font("Verdana", 20);
    private final Font shoutFont = Font.font("Verdana", 30);

    /**
     * Throws a pedestrian across the road.
     *
     * <p>
     * Three separate things happen at once, because one of them alone reads as a bug rather
     * than as an impact: a fine mist that carries a long way, heavier matter that arcs and
     * falls short, and a lasting stain on the tarmac that the road then carries away behind
     * the taxi.
     * </p>
     *
     * @param x    impact X
     * @param y    impact Y
     * @param size roughly how tall the pedestrian was, which scales the mess
     */
    void splat(double x, double y, double size) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        int mist = 52 + (int) (size / 2.2);
        for (int i = 0; i < mist; i++) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            // Biased forwards and upwards: the taxi is driving through them, not into them.
            p.vx = rng.nextDouble(-3.0, 13.0);
            p.vy = rng.nextDouble(-9.5, 1.5);
            p.size = rng.nextDouble(1.6, 5.0);
            p.life = rng.nextInt(26, 76);
            p.maxLife = p.life;
            p.colour = rng.nextBoolean() ? Color.web("#b3121b") : Color.web("#7d0a10");
            particles.add(p);
        }

        // The heavy stuff: fewer, bigger, slower, and it lands.
        int chunks = 9 + (int) (size / 12);
        for (int i = 0; i < chunks; i++) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            p.vx = rng.nextDouble(-1.5, 6.5);
            p.vy = rng.nextDouble(-6.0, -1.0);
            p.size = rng.nextDouble(5.5, 11.0);
            p.life = rng.nextInt(50, 96);
            p.maxLife = p.life;
            p.colour = rng.nextBoolean() ? Color.web("#68060c") : Color.web("#8f1a12");
            particles.add(p);
        }

        // The stain, and the streaks the tyres drag out of it.
        stain(x, y + size * 0.18, size * 2.1, size * 0.5, Color.color(0.42, 0.02, 0.05, 0.78));
        for (int i = 0; i < 4; i++) {
            stain(x + size * (0.5 + i * 0.55), y + size * (0.10 + rng.nextDouble(0.22)),
                    size * rng.nextDouble(0.5, 1.1), size * rng.nextDouble(0.10, 0.20),
                    Color.color(0.36, 0.02, 0.04, 0.5));
        }

        flash(Color.color(0.55, 0.0, 0.02, 0.30), 7);
        shake(size / 3.4);
    }

    /**
     * Lays a stain on the tarmac, which then scrolls away with the road.
     *
     * @param x      centre X
     * @param y      centre Y
     * @param width  how wide the mark is
     * @param height how deep the mark is
     * @param colour the mark's colour
     */
    void stain(double x, double y, double width, double height, Color colour) {
        Decal decal = new Decal();
        decal.x = x - width / 2;
        decal.y = y - height / 2;
        decal.width = width;
        decal.height = height;
        decal.colour = colour;
        decal.life = DECAL_STEPS;
        decals.add(decal);
    }

    /**
     * Animates a fare climbing into the taxi.
     *
     * @param image  the passenger sprite
     * @param fromX  where they were standing
     * @param fromY  where they were standing
     * @param width  their drawn width
     * @param height their drawn height
     * @param toX    the taxi door
     * @param toY    the taxi door
     */
    void board(Image image, double fromX, double fromY, double width, double height,
            double toX, double toY) {
        transfers.add(makeTransfer(image, fromX, fromY, width, height, toX, toY, false));
    }

    /**
     * Animates a fare getting out of the taxi and onto the kerb.
     *
     * <p>
     * Not simply {@link #board} run backwards. Somebody getting in is pulled towards the door
     * and shrinks away into it; somebody getting out steps down, grows back to full size and
     * arrives — so the two use different easing, a different arc and opposite fades.
     * </p>
     *
     * @param image  the passenger sprite
     * @param fromX  the taxi door
     * @param fromY  the taxi door
     * @param width  their drawn width once they are standing up
     * @param height their drawn height once they are standing up
     * @param toX    where they end up on the kerb
     * @param toY    where they end up on the kerb
     */
    void alight(Image image, double fromX, double fromY, double width, double height,
            double toX, double toY) {
        transfers.add(makeTransfer(image, fromX, fromY, width, height, toX, toY, true));
    }

    /**
     * Builds one fare-in-transit.
     *
     * @param image    the passenger sprite
     * @param fromX    start X
     * @param fromY    start Y
     * @param width    drawn width at full size
     * @param height   drawn height at full size
     * @param toX      end X
     * @param toY      end Y
     * @param outbound {@code true} when they are getting out
     * @return the assembled effect.
     */
    private static Transfer makeTransfer(Image image, double fromX, double fromY, double width,
            double height, double toX, double toY, boolean outbound) {
        Transfer transfer = new Transfer();
        transfer.image = image;
        transfer.fromX = fromX;
        transfer.fromY = fromY;
        transfer.toX = toX;
        transfer.toY = toY;
        transfer.width = width;
        transfer.height = height;
        transfer.outbound = outbound;
        transfer.arc = height * (outbound ? 0.42 : 0.28);
        transfer.life = TRANSFER_STEPS;
        transfer.maxLife = transfer.life;
        return transfer;
    }

    /**
     * Streaks a bullet across the screen.
     *
     * @param fromX the muzzle
     * @param fromY the muzzle
     * @param toX   where it ends up
     * @param toY   where it ends up
     */
    void tracer(double fromX, double fromY, double toX, double toY) {
        Tracer shot = new Tracer();
        shot.fromX = fromX;
        shot.fromY = fromY;
        shot.toX = toX;
        shot.toY = toY;
        shot.life = TRACER_STEPS;
        shot.maxLife = shot.life;
        tracers.add(shot);
    }

    /**
     * Throws a shower of sparks, for a round that went into the tarmac or into a wheel arch.
     *
     * @param x      where it struck
     * @param y      where it struck
     * @param colour the spark colour
     * @param count  how many
     */
    void sparks(double x, double y, Color colour, int count) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            p.vx = rng.nextDouble(-6.5, 6.5);
            p.vy = rng.nextDouble(-6.0, -0.5);
            p.size = rng.nextDouble(1.5, 3.6);
            p.life = rng.nextInt(12, 30);
            p.maxLife = p.life;
            p.colour = colour;
            particles.add(p);
        }
    }

    /**
     * Washes the whole frame with a colour that fades out over the following steps.
     *
     * @param colour the wash colour, including its peak alpha
     * @param steps  how long it lasts
     */
    void flash(Color colour, int steps) {
        // Never let a weaker flash cut a stronger one short.
        if (flashLife > 0 && flashColour != null
                && flashColour.getOpacity() > colour.getOpacity()) {
            return;
        }
        flashColour = colour;
        flashLife = steps;
        flashMaxLife = steps;
    }

    /**
     * Floats a short message upwards from a point in the world.
     *
     * @param message the text
     * @param x       start X
     * @param y       start Y
     * @param colour  the text colour
     * @param large   {@code true} for impact messages, {@code false} for running totals
     */
    void floatText(String message, double x, double y, Color colour, boolean large) {
        FloatingText t = new FloatingText();
        t.message = message;
        t.x = x;
        t.y = y;
        t.colour = colour;
        t.large = large;
        t.life = large ? 70 : 46;
        t.maxLife = t.life;
        texts.add(t);
    }

    /**
     * Kicks the camera.
     *
     * @param magnitude how hard, in pixels
     */
    void shake(double magnitude) {
        shakeMagnitude = Math.min(26, Math.max(shakeMagnitude, magnitude));
    }

    /**
     * Advances every effect by one step.
     *
     * @param scroll how far the road moved this step, so that marks on it travel with it
     */
    void update(double scroll) {
        for (Iterator<Particle> it = particles.iterator(); it.hasNext();) {
            Particle p = it.next();
            p.x += p.vx;
            p.y += p.vy;
            p.vy += GRAVITY;
            p.life--;
            if (p.life <= 0) {
                it.remove();
            }
        }

        for (Iterator<Decal> it = decals.iterator(); it.hasNext();) {
            Decal d = it.next();
            d.x += scroll;
            d.life--;
            if (d.life <= 0 || d.x + d.width < 0) {
                it.remove();
            }
        }

        for (Iterator<FloatingText> it = texts.iterator(); it.hasNext();) {
            FloatingText t = it.next();
            t.y -= t.large ? 0.9 : 1.3;
            t.life--;
            if (t.life <= 0) {
                it.remove();
            }
        }

        for (Iterator<Transfer> it = transfers.iterator(); it.hasNext();) {
            Transfer b = it.next();
            b.life--;
            if (b.life <= 0) {
                it.remove();
            }
        }

        for (Iterator<Tracer> it = tracers.iterator(); it.hasNext();) {
            Tracer t = it.next();
            t.life--;
            if (t.life <= 0) {
                it.remove();
            }
        }

        if (flashLife > 0) {
            flashLife--;
        }

        if (shakeMagnitude > SHAKE_FLOOR) {
            shakeX = ThreadLocalRandom.current().nextDouble(-shakeMagnitude, shakeMagnitude);
            shakeY = ThreadLocalRandom.current().nextDouble(-shakeMagnitude, shakeMagnitude);
            shakeMagnitude *= SHAKE_DECAY;
        } else {
            shakeMagnitude = 0;
            shakeX = 0;
            shakeY = 0;
        }
    }

    /**
     * Draws the marks that lie on the road, underneath the traffic.
     *
     * @param gc the graphics context
     */
    void renderGround(GraphicsContext gc) {
        if (decals.isEmpty()) {
            return;
        }
        gc.save();
        for (Decal d : decals) {
            gc.setGlobalAlpha(Math.min(1.0, d.life / (double) DECAL_FADE_STEPS));
            gc.setFill(d.colour);
            gc.fillOval(d.x, d.y, d.width, d.height);
        }
        gc.restore();
        gc.setGlobalAlpha(1.0);
    }

    /**
     * Draws the effects that live in the world, in front of the traffic.
     *
     * @param gc the graphics context
     */
    void renderWorld(GraphicsContext gc) {
        for (Transfer b : transfers) {
            drawTransfer(gc, b);
        }

        for (Tracer t : tracers) {
            double fade = t.life / (double) t.maxLife;
            gc.save();
            gc.setGlobalAlpha(fade);
            gc.setLineWidth(5);
            gc.setStroke(Color.color(1.0, 0.86, 0.45, 0.45));
            gc.strokeLine(t.fromX, t.fromY, t.toX, t.toY);
            gc.setLineWidth(1.6);
            gc.setStroke(Color.web("#fff6d5"));
            gc.strokeLine(t.fromX, t.fromY, t.toX, t.toY);
            gc.restore();
        }
        gc.setLineWidth(1);

        for (Particle p : particles) {
            double fade = p.life / (double) p.maxLife;
            gc.setGlobalAlpha(Math.min(1.0, fade * 1.6));
            gc.setFill(p.colour);
            gc.fillOval(p.x, p.y, p.size, p.size);
        }
        gc.setGlobalAlpha(1.0);
    }

    /**
     * Draws one fare part way in or out of the taxi.
     *
     * @param gc the graphics context
     * @param b  the fare in transit
     */
    private void drawTransfer(GraphicsContext gc, Transfer b) {
        double t = 1.0 - (b.life / (double) b.maxLife);

        // Getting in is a single smooth pull towards the door; getting out is a quick step
        // down that settles, so the two are eased differently.
        double ease = b.outbound
                ? 1.0 - Math.pow(1.0 - t, 3)
                : t * t * (3.0 - 2.0 * t);

        double bx = b.fromX + (b.toX - b.fromX) * ease;
        // The hop: they rise off the seat or off the step and come down at the far end.
        double by = b.fromY + (b.toY - b.fromY) * ease - Math.sin(Math.PI * ease) * b.arc;

        double scale = b.outbound ? 0.45 + 0.55 * ease : 1.0 - 0.58 * ease;
        double alpha = b.outbound
                ? Math.min(1.0, 0.30 + t * 3.0)
                : Math.max(0.15, 1.0 - Math.max(0, t - 0.55) * 2.0);
        // A little sway through the middle of the move, so they turn as they step.
        double tilt = Math.sin(Math.PI * ease) * (b.outbound ? 11 : -8);

        double drawWidth = b.width * scale;
        double drawHeight = b.height * scale;

        gc.save();
        gc.setGlobalAlpha(alpha);
        gc.translate(bx + drawWidth / 2, by + drawHeight);
        gc.rotate(tilt);
        gc.drawImage(b.image, -drawWidth / 2, -drawHeight, drawWidth, drawHeight);
        gc.restore();
    }

    /**
     * Draws the floating text, on top of everything in the world.
     *
     * @param gc the graphics context
     */
    void renderText(GraphicsContext gc) {
        gc.setTextAlign(TextAlignment.CENTER);
        for (FloatingText t : texts) {
            double fade = t.life / (double) t.maxLife;
            gc.setGlobalAlpha(Math.min(1.0, fade * 2.2));
            gc.setFont(t.large ? shoutFont : floatFont);

            // A dark rim keeps the text legible over both tarmac and sky.
            gc.setFill(Color.color(0, 0, 0, 0.55));
            gc.fillText(t.message, t.x + 2, t.y + 2);
            gc.setFill(t.colour);
            gc.fillText(t.message, t.x, t.y);
        }
        gc.setGlobalAlpha(1.0);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    /**
     * Washes the frame after an impact.
     *
     * <p>
     * Drawn outside the shaken world transform: the flash is something happening to the
     * camera, so a shaking one would leave unpainted edges.
     * </p>
     *
     * @param gc the graphics context
     */
    void renderFlash(GraphicsContext gc) {
        if (flashLife <= 0 || flashColour == null) {
            return;
        }
        double fade = flashLife / (double) flashMaxLife;
        gc.save();
        gc.setGlobalAlpha(fade);
        gc.setFill(flashColour);
        gc.fillRect(0, 0, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
        gc.restore();
        gc.setGlobalAlpha(1.0);
    }

    /**
     * Returns the current horizontal camera offset.
     *
     * @return the shake offset in pixels.
     */
    double getShakeX() {
        return shakeX;
    }

    /**
     * Returns the current vertical camera offset.
     *
     * @return the shake offset in pixels.
     */
    double getShakeY() {
        return shakeY;
    }

    /**
     * Drops every effect, used when a run is reset.
     */
    void clear() {
        particles.clear();
        texts.clear();
        transfers.clear();
        decals.clear();
        tracers.clear();
        shakeMagnitude = 0;
        shakeX = 0;
        shakeY = 0;
        flashLife = 0;
        flashColour = null;
    }

    /** A single piece of debris. */
    private static final class Particle {
        double x;
        double y;
        double vx;
        double vy;
        double size;
        int life;
        int maxLife;
        Color colour;
    }

    /** A mark lying on the road surface, travelling with it. */
    private static final class Decal {
        double x;
        double y;
        double width;
        double height;
        int life;
        Color colour;
    }

    /** A message drifting upwards out of the world. */
    private static final class FloatingText {
        String message;
        double x;
        double y;
        int life;
        int maxLife;
        boolean large;
        Color colour;
    }

    /** A fare on their way into or out of the taxi. */
    private static final class Transfer {
        Image image;
        double fromX;
        double fromY;
        double toX;
        double toY;
        double width;
        double height;
        double arc;
        boolean outbound;
        int life;
        int maxLife;
    }

    /** A round in flight. */
    private static final class Tracer {
        double fromX;
        double fromY;
        double toX;
        double toY;
        int life;
        int maxLife;
    }
}
