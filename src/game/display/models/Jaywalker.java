package game.display.models;

import java.util.concurrent.ThreadLocalRandom;

import game.logic.Visitor;
import game.utility.EPassenger;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.paint.Color;

/**
 * A pedestrian who has decided the gap in traffic is big enough.
 *
 * <p>
 * A jaywalker crosses between the two pavements while the road scrolls past. It is drawn
 * with the same perspective trick as everything else — small and pale near the top of the
 * screen, large and solid near the bottom — so that a figure crossing reads as walking
 * towards or away from the camera rather than sliding up and down a flat image.
 * </p>
 */
public final class Jaywalker extends Sprite {

    /** Y coordinate of the far pavement. */
    private static final double FAR_Y = EPassenger.PASSENGER_TOP.getLocation();
    /** Y coordinate of the near pavement. */
    private static final double NEAR_Y = EPassenger.PASSENGER_BOTTOM.getLocation();
    /** Drawn height on the far pavement. */
    private static final double FAR_HEIGHT = EPassenger.PASSENGER_TOP.getHeightScale();
    /** Drawn height on the near pavement. */
    private static final double NEAR_HEIGHT = EPassenger.PASSENGER_BOTTOM.getHeightScale();

    /** Slowest crossing, as a fraction of the road per step. */
    private static final double MIN_SPEED = 0.0026;
    /** Fastest crossing, as a fraction of the road per step. */
    private static final double MAX_SPEED = 0.0062;
    /** How long the remains stay on the road after a hit, in steps. */
    private static final int SPLAT_LINGER_STEPS = 150;
    /** Fraction of the sprite's width used for the hit box. */
    private static final double BOUND_WIDTH_FRACTION = 0.85;
    /** Fraction of the sprite's height used for the hit box, measured from the feet up. */
    private static final double BOUND_HEIGHT_FRACTION = 0.5;

    /** 0 means "at the pavement it set off from", 1 means "safely across". */
    private double progress;
    /** How far the crossing advances each step. */
    private final double speed;
    /** True when the pedestrian set off from the far pavement. */
    private final boolean fromFar;
    /** Bobs the walk cycle so the figure does not slide stiffly across. */
    private double walkPhase;

    private boolean splatted = false;
    private int splatTimer = 0;

    /**
     * Creates a pedestrian starting on one of the two pavements.
     *
     * @param x       where on the road the crossing happens
     * @param fromFar {@code true} to set off from the far pavement
     */
    public Jaywalker(int x, boolean fromFar) {
        super(x, (int) (fromFar ? FAR_Y : NEAR_Y));
        this.fromFar = fromFar;
        this.progress = 0;
        this.speed = ThreadLocalRandom.current().nextDouble(MIN_SPEED, MAX_SPEED);
        this.walkPhase = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        place();
    }

    /**
     * Reports whether this pedestrian has been run down.
     *
     * @return {@code true} once hit.
     */
    public boolean isSplatted() {
        return splatted;
    }

    /**
     * Marks the pedestrian as run down.
     *
     * @return {@code true} if this was the hit that got them, {@code false} if they were
     *         already down — which stops one collision being counted on many frames.
     */
    public boolean splat() {
        if (splatted) {
            return false;
        }
        splatted = true;
        splatTimer = SPLAT_LINGER_STEPS;
        // The remains are flat on the tarmac and no longer collidable.
        myBound.setWidth(0);
        myBound.setHeight(0);
        return true;
    }

    /**
     * Returns how far across the road the pedestrian is.
     *
     * @return 0 at the starting kerb, 1 at the far kerb.
     */
    public double getProgress() {
        return progress;
    }

    /**
     * Returns the height the figure is currently drawn at, used to size the mess they make.
     *
     * @return the drawn height in pixels.
     */
    public double currentHeight() {
        return FAR_HEIGHT + (NEAR_HEIGHT - FAR_HEIGHT) * depth();
    }

    @Override
    public void tick() {
        if (splatted) {
            splatTimer--;
            return;
        }
        progress += speed;
        walkPhase += 0.34;
        place();
    }

    @Override
    public boolean isExpired() {
        return splatted ? splatTimer <= 0 : progress >= 1.0;
    }

    @Override
    public boolean wrapsAround() {
        // A pedestrian crosses once. They do not loop back around for another go.
        return false;
    }

    /**
     * Positions and sizes the figure for its current point in the crossing.
     */
    private void place() {
        double height = currentHeight();
        fitToHeight(height);

        int newY = (int) Math.round(FAR_Y + (NEAR_Y - FAR_Y) * depth());
        this.y = newY;

        // The hit box hugs the figure's lower half so that clipping the space above their
        // head does not count as running them over.
        double width = renderWidth();
        myBound.setWidth(width * BOUND_WIDTH_FRACTION);
        myBound.setHeight(height * BOUND_HEIGHT_FRACTION);
        myBound.setX((int) Math.round(x + width * (1 - BOUND_WIDTH_FRACTION) / 2));
        myBound.setY((int) Math.round(newY + height * (1 - BOUND_HEIGHT_FRACTION)));
    }

    /**
     * Converts crossing progress into a depth between the two pavements.
     *
     * @return 0 at the far pavement, 1 at the near pavement.
     */
    private double depth() {
        double clamped = Math.min(1.0, Math.max(0.0, progress));
        return fromFar ? clamped : 1.0 - clamped;
    }

    @Override
    public void setX(int x) {
        this.x = x;
        // place() owns the hit box, so only nudge it by the same amount the sprite moved.
        double width = renderWidth();
        myBound.setX((int) Math.round(x + width * (1 - BOUND_WIDTH_FRACTION) / 2));
    }

    @Override
    public void draw(GraphicsContext gc) {
        if (myImage == null) {
            return;
        }
        double width = renderWidth();
        double height = renderHeight();

        if (splatted) {
            drawRemains(gc, width, height);
            return;
        }

        // A slight lean, alternating with the walk cycle, reads as hurrying across.
        double lean = Math.sin(walkPhase) * 3;
        gc.save();
        gc.translate(x + width / 2, y + height);
        gc.rotate(lean);
        gc.drawImage(myImage, -width / 2, -height, width, height);
        gc.restore();
    }

    /**
     * Draws what is left of a pedestrian who did not make it, fading as it is left behind.
     *
     * @param gc     the graphics context
     * @param width  the figure's drawn width
     * @param height the figure's drawn height
     */
    private void drawRemains(GraphicsContext gc, double width, double height) {
        double fade = Math.min(1.0, splatTimer / (double) SPLAT_LINGER_STEPS);

        gc.save();
        gc.setGlobalAlpha(fade);

        // The stain: a spreading smear on the tarmac.
        gc.setFill(Color.color(0.45, 0.02, 0.05, 0.85));
        gc.fillOval(x - width * 0.35, y + height * 0.72, width * 1.8, height * 0.34);
        gc.setFill(Color.color(0.62, 0.05, 0.08, 0.7));
        gc.fillOval(x - width * 0.1, y + height * 0.78, width * 1.25, height * 0.22);

        // The figure itself, flattened onto the road and drained of colour.
        ColorAdjust pale = new ColorAdjust();
        pale.setSaturation(-0.6);
        pale.setBrightness(-0.35);
        gc.setEffect(pale);
        gc.translate(x + width / 2, y + height);
        gc.scale(1.5, 0.28);
        gc.drawImage(myImage, -width / 2, -height, width, height);
        gc.setEffect(null);

        gc.restore();
    }

    /**
     * Acceptor for the visitor pattern.
     */
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
