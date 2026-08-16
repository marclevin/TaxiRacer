package game.display.models;

import game.logic.Visitor;
import game.utility.EPolicePositions;
import game.utility.ESettings;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * The pursuing police car, and the two officers in it.
 *
 * <p>
 * The police are tracked by a signed distance rather than by screen position: a negative
 * distance means they are still off screen and closing, and it is shown to the player as
 * "metres behind". Only once the distance reaches zero does the car become a real,
 * collidable object on the road.
 * </p>
 *
 * <p>
 * The distance is a {@code double} because difficulty profiles move it by fractions of a
 * metre per step; rounding each of those to an integer would quietly floor small drifts to
 * zero and make gentle difficulties behave identically to harsh ones.
 * </p>
 *
 * <p>
 * Once they are close enough to be seen, the chase stops being a single number. The car
 * leans across lanes to follow the taxi rather than snapping into its lane, it surges and
 * drops back instead of closing at a constant rate, and the officer in the back window leans
 * out to take a shot at the taxi's tyres. That shot is deliberately slow to line up and
 * locks onto a lane before it is taken, so it is always survivable by changing lane — the
 * chase asks the player for something to do rather than only for something to watch.
 * </p>
 */
public class Police extends Sprite {

    /** Default gap at the start of a run, in metres. */
    public static final double DEFAULT_START_DISTANCE = -ESettings.SCENE_WIDTH.getVal();

    /** The four road lanes; the enum's last entry is the mirror portrait, not a lane. */
    private static final int LANE_COUNT = 4;
    /** Fraction of the remaining lane gap closed each step. Low enough to read as a swerve. */
    private static final double LANE_FOLLOW = 0.085;

    // ---- Taking a shot ----
    /**
     * Widest screen gap, in pixels, at which the officer will bother lining a shot up.
     *
     * <p>
     * Chosen so that the car is already most of the way onto the screen when the pistol
     * comes out. A wider range let them open fire from a car that was still a sliver at the
     * left edge, which gave the player a warning banner with nothing to look at.
     * </p>
     */
    private static final double AIM_MAX_GAP = 470;
    /** Closest they will shoot from; nearer than this they are going for contact instead. */
    private static final double AIM_MIN_GAP = 90;
    /** How long the officer spends lining a shot up, in steps. */
    private static final int AIM_STEPS = 78;
    /**
     * Steps of the wind-up during which the aim is frozen on one lane.
     *
     * <p>
     * The lane is tracked live until this point and locked afterwards, so swerving early
     * achieves nothing and swerving late is what saves the tyre. That is the whole skill in
     * it: the dodge has to be timed rather than merely performed.
     * </p>
     */
    private static final int AIM_LOCK_STEPS = 26;
    /** Steps between shots. */
    private static final int SHOT_COOLDOWN = 170;
    /** Steps between shots while they are furious about a pedestrian. */
    private static final int ENRAGED_COOLDOWN = 100;
    /** How long the muzzle flash is drawn for. */
    private static final int MUZZLE_STEPS = 7;

    // ---- Surging ----
    /** Screen gap, in pixels, within which the police start lunging for the taxi. */
    private static final double SURGE_RANGE = 780;
    /** Ground gained per step during a lunge. */
    private static final double SURGE_GAIN = 2.1;
    /** How long a lunge lasts, in steps. */
    private static final int SURGE_STEPS = 34;
    /** Ground given back per step while the engine recovers from a lunge. */
    private static final double SURGE_REST_LOSS = -1.15;
    /** How long the recovery lasts, in steps. */
    private static final int SURGE_REST_STEPS = 52;
    /** Quiet steps between one lunge and the next. */
    private static final int SURGE_CALM_STEPS = 90;

    // ---- Where the crew sit on the sprite, as fractions of the drawn car ----
    /** Driver's window, measured from the back of the car. */
    private static final double DRIVER_U = 0.560;
    /** Rear window, where the officer with the pistol sits. */
    private static final double SHOOTER_U = 0.410;
    /** Top of an officer's head, measured from the roof. */
    private static final double OFFICER_V = 0.150;
    /** Head width, as a fraction of the car's width. */
    private static final double OFFICER_W = 0.078;
    /** Head height, as a fraction of the car's height. */
    private static final double OFFICER_H = 0.230;
    /** Fraction of the passenger sprite that is head and shoulders. */
    private static final double HEAD_FRACTION = 0.56;

    /** What just came out of the barrel, if anything. */
    public enum Shot {
        /** Nobody fired this step. */
        NONE,
        /** Fired, but the taxi had already changed lane. */
        MISS,
        /** Fired and connected. */
        HIT
    }

    /** What the crew are currently doing. */
    private enum Threat {
        /** Driving, too far back to try anything. */
        CHASING,
        /** Leaning out and lining a shot up. */
        AIMING,
        /** Reloading and getting back in the window. */
        RECOVERING
    }

    private boolean isPrime = true;
    private double distance = DEFAULT_START_DISTANCE;
    private double startDistance = DEFAULT_START_DISTANCE;
    private int enrageTimer = 0;
    private int enrageDuration = 0;
    private Image primeImage = null;
    private Image secondImage = null;
    private Image occupantImage = null;

    /** Continuous lane index, eased towards the taxi's lane rather than snapped to it. */
    private double lanePos = 2;
    /** Drives the flashing light bar and the weave. */
    private double sirenPhase = 0;

    private Threat threat = Threat.CHASING;
    private int aimTimer = 0;
    private int cooldown = 0;
    private int muzzleTimer = 0;
    private int lockedLane = 0;
    private Shot pendingShot = Shot.NONE;

    private int surgeTimer = 0;
    private int restTimer = 0;
    private int calmTimer = 0;

    /**
     * Constructor for the Police class
     *
     * @param x X coordinate of the police
     * @param y Y coordinate of the police
     */
    public Police(int x, int y) {
        super(x, y);
    }

    /**
     * Sets the image set of the police
     *
     * @param prime     The first image of the police
     * @param secondary The second image of the police
     */
    public void setImageSet(Image prime, Image secondary) {
        this.primeImage = prime;
        this.secondImage = secondary;
        this.isPrime = true;
        setImage(prime);
    }

    /**
     * Supplies the sprite used to draw the officers sitting in the car.
     *
     * @param image the passenger sprite, which is cropped to head and shoulders
     */
    public void setOccupantImage(Image image) {
        this.occupantImage = image;
    }

    /**
     * Checks if the police are still off screen.
     *
     * @return True if the police are off screen, false otherwise
     */
    public boolean isHidden() {
        return (this.distance < 0);
    }

    /**
     * Changes the distance of the police from the Taxi
     *
     * @param delta Distance to add, in metres
     */
    public void changeDistance(double delta) {
        this.distance += delta;
    }

    /**
     * Gets the distance of the police from the taxi
     *
     * @return The distance of the police from the taxi
     */
    public double getDistance() {
        return this.distance;
    }

    /**
     * Gets the gap as a whole number of metres, for display.
     *
     * @return how many metres behind the police are; zero once they are alongside.
     */
    public int getMetresBehind() {
        return (int) Math.max(0, Math.round(-distance));
    }

    /**
     * Returns how close the police are as a fraction, for meters and warning colours.
     *
     * @return 0 when they are as far back as they started, 1 when they are alongside.
     */
    public double getCloseness() {
        if (startDistance >= 0) {
            return 1;
        }
        double fraction = 1.0 - (distance / startDistance);
        return Math.min(1.0, Math.max(0.0, fraction));
    }

    /**
     * Sends the police back to their starting distance, ready for a fresh run.
     *
     * @param start how far behind they should start, in metres (a positive number)
     */
    public void resetChase(double start) {
        this.startDistance = -Math.abs(start);
        this.distance = this.startDistance;
        this.enrageTimer = 0;
        this.enrageDuration = 0;
        this.threat = Threat.CHASING;
        this.aimTimer = 0;
        this.cooldown = 0;
        this.muzzleTimer = 0;
        this.pendingShot = Shot.NONE;
        this.surgeTimer = 0;
        this.restTimer = 0;
        this.calmTimer = 0;
        this.sirenPhase = 0;
        this.lanePos = game.logic.InputHandler.DEFAULT_LANE;
        this.lockedLane = game.logic.InputHandler.DEFAULT_LANE;
        setX((int) this.startDistance);
        scale(EPolicePositions.values()[game.logic.InputHandler.DEFAULT_LANE]);
    }

    /**
     * Whips the police into a fury after the taxi runs somebody down.
     *
     * @param steps how long the fury lasts
     */
    public void enrage(int steps) {
        this.enrageTimer = Math.max(this.enrageTimer, steps);
        // Remember what a full tank of fury looks like, so the meter empties honestly
        // whatever duration the current difficulty uses.
        this.enrageDuration = Math.max(this.enrageDuration, this.enrageTimer);
        // Furious officers do not wait out the rest of a reload.
        this.cooldown = Math.min(this.cooldown, ENRAGED_COOLDOWN);
    }

    /**
     * Reports whether the police are currently enraged.
     *
     * @return true while the fury lasts.
     */
    public boolean isEnraged() {
        return enrageTimer > 0;
    }

    /**
     * Returns how much of the fury is left, for driving the warning display.
     *
     * @return a fraction between 0 and 1.
     */
    public double getEnrageFraction() {
        if (enrageDuration <= 0) {
            return 0;
        }
        return Math.min(1.0, enrageTimer / (double) enrageDuration);
    }

    /**
     * Runs the fury timer down by one step.
     */
    public void tickEnrage() {
        if (enrageTimer > 0) {
            enrageTimer--;
        }
        if (enrageTimer == 0) {
            enrageDuration = 0;
        }
    }

    // ------------------------------------------------------------------
    // Pursuit
    // ------------------------------------------------------------------

    /**
     * Eases the car towards the taxi's lane and re-scales it for the lane it is now in.
     *
     * <p>
     * The car used to be snapped straight into the player's lane, which read as teleporting
     * sideways: a lane change put the police somewhere they had never driven through. Easing
     * across is both truer and more useful, because the lag is what gives a lane change its
     * moment of advantage.
     * </p>
     *
     * @param playerLane the lane index the taxi currently occupies
     */
    public void followLane(int playerLane) {
        int target = Math.min(LANE_COUNT - 1, Math.max(0, playerLane));
        lanePos += (target - lanePos) * LANE_FOLLOW;
        sirenPhase += isEnraged() ? 0.30 : 0.19;

        EPolicePositions[] lanes = EPolicePositions.values();
        int low = (int) Math.floor(lanePos);
        low = Math.min(LANE_COUNT - 1, Math.max(0, low));
        int high = Math.min(LANE_COUNT - 1, low + 1);
        double blend = Math.min(1.0, Math.max(0.0, lanePos - low));

        double width = lerp(lanes[low].getWidthScale(), lanes[high].getWidthScale(), blend);
        double location = lerp(lanes[low].getLocation(), lanes[high].getLocation(), blend);

        // A slight weave once they are hunting, so a car holding station behind the taxi
        // still looks driven rather than towed.
        if (threat != Threat.CHASING || surgeTimer > 0) {
            location += Math.sin(sirenPhase * 0.9) * 3.0;
        }

        setY((int) Math.round(location));
        fitToWidth(width);
        this.myBound.setWidth(width);
        this.myBound.setHeight(width * 0.5);
    }

    /**
     * Advances the crew's decision making by one step.
     *
     * @param playerLane the lane the taxi is in right now
     * @param gapPixels  how far ahead of the police car the taxi is, on screen
     */
    public void updateChase(int playerLane, double gapPixels) {
        if (muzzleTimer > 0) {
            muzzleTimer--;
        }

        boolean inRange = !isHidden() && gapPixels <= AIM_MAX_GAP && gapPixels >= AIM_MIN_GAP;

        switch (threat) {
            case CHASING:
                if (inRange) {
                    threat = Threat.AIMING;
                    aimTimer = isEnraged() ? (int) (AIM_STEPS * 0.75) : AIM_STEPS;
                    lockedLane = playerLane;
                }
                break;

            case AIMING:
                if (!inRange) {
                    // Lost the shot. Sit back down rather than firing blind.
                    threat = Threat.CHASING;
                    aimTimer = 0;
                    break;
                }
                if (aimTimer > AIM_LOCK_STEPS) {
                    lockedLane = playerLane;
                }
                if (--aimTimer <= 0) {
                    pendingShot = lockedLane == playerLane ? Shot.HIT : Shot.MISS;
                    muzzleTimer = MUZZLE_STEPS;
                    cooldown = isEnraged() ? ENRAGED_COOLDOWN : SHOT_COOLDOWN;
                    threat = Threat.RECOVERING;
                }
                break;

            case RECOVERING:
            default:
                if (--cooldown <= 0) {
                    threat = Threat.CHASING;
                }
                break;
        }
    }

    /**
     * Takes the shot fired this step, if one was.
     *
     * @return what happened, and {@link Shot#NONE} on every other step.
     */
    public Shot consumeShot() {
        Shot shot = pendingShot;
        pendingShot = Shot.NONE;
        return shot;
    }

    /**
     * Reports whether an officer is currently lining a shot up.
     *
     * @return {@code true} while the pistol is out.
     */
    public boolean isAiming() {
        return threat == Threat.AIMING;
    }

    /**
     * Returns how far through the wind-up the officer is.
     *
     * @return 0 as the pistol comes up, 1 as it goes off.
     */
    public double getAimProgress() {
        if (threat != Threat.AIMING) {
            return 0;
        }
        return 1.0 - Math.min(1.0, aimTimer / (double) AIM_STEPS);
    }

    /**
     * Reports whether the aim has stopped tracking and settled on a lane.
     *
     * @return {@code true} once swerving is the only thing that will save the tyre.
     */
    public boolean isLockedOn() {
        return threat == Threat.AIMING && aimTimer <= AIM_LOCK_STEPS;
    }

    /**
     * Returns the lane the pistol is pointed at.
     *
     * @return a lane index between 0 and 3.
     */
    public int getLockedLane() {
        return lockedLane;
    }

    /**
     * Advances the lunge cycle and reports what it does to the gap this step.
     *
     * <p>
     * A chase that closes at a constant rate has no shape to it; the player either escapes or
     * does not, and knows which within a second. Lunging and dropping back turns the same
     * average pressure into a series of near-misses, which is the part worth playing.
     * </p>
     *
     * @param gapPixels how far ahead of the police car the taxi is, on screen
     * @return the metres to add to the gap this step.
     */
    public double tickSurge(double gapPixels) {
        if (isHidden() || gapPixels > SURGE_RANGE) {
            surgeTimer = 0;
            restTimer = 0;
            calmTimer = 0;
            return 0;
        }

        if (surgeTimer > 0) {
            if (--surgeTimer == 0) {
                restTimer = SURGE_REST_STEPS;
            }
            return SURGE_GAIN;
        }
        if (restTimer > 0) {
            if (--restTimer == 0) {
                calmTimer = SURGE_CALM_STEPS;
            }
            return SURGE_REST_LOSS;
        }
        if (calmTimer > 0) {
            calmTimer--;
            return 0;
        }

        surgeTimer = SURGE_STEPS;
        return SURGE_GAIN;
    }

    /**
     * Reports whether the car is mid-lunge, for the HUD.
     *
     * @return {@code true} while they are flooring it.
     */
    public boolean isSurging() {
        return surgeTimer > 0;
    }

    /**
     * Returns the X coordinate the pistol is fired from.
     *
     * @return the muzzle's screen X.
     */
    public double getMuzzleX() {
        return x + renderWidth() * (SHOOTER_U + OFFICER_W * 3.4);
    }

    /**
     * Returns the Y coordinate the pistol is fired from.
     *
     * @return the muzzle's screen Y.
     */
    public double getMuzzleY() {
        return y + renderHeight() * (OFFICER_V + OFFICER_H * 0.30);
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    /**
     * Scales the police based on {@code EPolicePositions}.
     *
     * @param position The position of the police.
     */
    public void scale(EPolicePositions position) {
        double width = position.getWidthScale();
        this.setY(position.getLocation());
        fitToWidth(width);
        this.myBound.setWidth(width);
        this.myBound.setHeight(width * 0.5);
    }

    /**
     * Swaps the image of the police, used to make the car appear like it is moving.
     */
    public void swapImage() {
        isPrime = !isPrime;
        setImage(isPrime ? primeImage : secondImage);
    }

    @Override
    public void draw(GraphicsContext gc) {
        super.draw(gc);

        double width = renderWidth();
        double height = renderHeight();
        if (width <= 0) {
            return;
        }

        drawLightBar(gc, width, height);
        drawCrew(gc, width, height);
    }

    /**
     * Draws the flashing bar on the roof.
     *
     * @param gc     the graphics context
     * @param width  the car's drawn width
     * @param height the car's drawn height
     */
    private void drawLightBar(GraphicsContext gc, double width, double height) {
        // Hard alternation rather than a smooth fade: a real light bar strobes, and a sine
        // wave here reads as a gentle glow instead of an emergency.
        boolean blueOn = Math.sin(sirenPhase) >= 0;

        double barWidth = width * 0.055;
        double barHeight = height * 0.085;
        double barY = y + height * 0.055;
        double blueX = x + width * 0.415;
        double redX = x + width * 0.505;

        drawLamp(gc, blueX, barY, barWidth, barHeight, Color.web("#2f7bff"), blueOn);
        drawLamp(gc, redX, barY, barWidth, barHeight, Color.web("#ff2f3d"), !blueOn);
    }

    /**
     * Draws one lamp of the light bar, lit or dark.
     *
     * @param gc     the graphics context
     * @param lampX  the lamp's left edge
     * @param lampY  the lamp's top edge
     * @param width  the lamp's width
     * @param height the lamp's height
     * @param colour the lamp's colour
     * @param lit    whether it is currently on
     */
    private void drawLamp(GraphicsContext gc, double lampX, double lampY, double width,
            double height, Color colour, boolean lit) {
        gc.save();
        if (lit) {
            gc.setGlobalAlpha(0.32);
            gc.setFill(colour);
            gc.fillOval(lampX - width * 1.2, lampY - height * 1.1, width * 3.4, height * 3.2);
            gc.setGlobalAlpha(1.0);
            gc.setFill(colour.brighter());
        } else {
            gc.setFill(colour.darker().darker());
        }
        gc.fillRoundRect(lampX, lampY, width, height, width * 0.6, width * 0.6);
        gc.restore();
    }

    /**
     * Draws the driver and the officer beside them.
     *
     * @param gc     the graphics context
     * @param width  the car's drawn width
     * @param height the car's drawn height
     */
    private void drawCrew(GraphicsContext gc, double width, double height) {
        if (occupantImage == null) {
            return;
        }

        double headWidth = width * OFFICER_W;
        double headHeight = height * OFFICER_H;
        double headY = y + height * OFFICER_V;

        drawOfficer(gc, x + width * DRIVER_U, headY, headWidth, headHeight);

        // The shooter climbs half out of the window as the pistol comes up, so the wind-up is
        // visible on the car itself and not only in the warning text.
        double lean = isAiming() ? Math.min(1.0, getAimProgress() * 1.6) : 0;
        if (muzzleTimer > 0) {
            lean = 1.0;
        }
        double shooterX = x + width * SHOOTER_U + width * 0.035 * lean;
        double shooterY = headY - headHeight * 0.30 * lean;

        drawOfficer(gc, shooterX, shooterY, headWidth, headHeight);

        if (lean > 0.05) {
            drawPistolArm(gc, shooterX, shooterY, headWidth, headHeight, lean);
        }
    }

    /**
     * Draws one officer: head and shoulders through the glass, under a peaked cap.
     *
     * <p>
     * The cap is drawn rather than supplied as a sprite so that it scales with the car at all
     * four lane sizes, and so that the same passenger image can play a fare in the taxi and a
     * constable in the pursuit car without a second asset.
     * </p>
     *
     * @param gc     the graphics context
     * @param centre the head's centre X
     * @param top    the head's top edge
     * @param width  the head's drawn width
     * @param height the head's drawn height
     */
    private void drawOfficer(GraphicsContext gc, double centre, double top, double width,
            double height) {
        double left = centre - width / 2;

        gc.save();
        gc.setGlobalAlpha(0.94);
        gc.drawImage(occupantImage,
                0, 0, occupantImage.getWidth(), occupantImage.getHeight() * HEAD_FRACTION,
                left, top, width, height);
        gc.restore();

        // The cap: a navy crown, a band, a peak pointing down the road, and a badge.
        double crownWidth = width * 1.30;
        double crownHeight = height * 0.46;
        double crownX = centre - crownWidth / 2;
        double crownY = top - crownHeight * 0.30;

        // An oval crown with a band laid across its waist: the band hides the bottom of the
        // oval, so what is left reads as a domed cap rather than as an egg.
        gc.setFill(Color.web("#16233f"));
        gc.fillOval(crownX, crownY, crownWidth, crownHeight * 1.55);

        gc.setFill(Color.web("#0d162b"));
        gc.fillRect(crownX, crownY + crownHeight * 0.80, crownWidth, crownHeight * 0.34);

        // The peak juts towards the front of the car, which is the way they are looking.
        gc.setFill(Color.web("#0a1020"));
        gc.fillOval(centre - crownWidth * 0.10, crownY + crownHeight * 0.96,
                crownWidth * 0.86, crownHeight * 0.34);

        gc.setFill(Color.web("#ffd166"));
        gc.fillOval(centre - width * 0.10, crownY + crownHeight * 0.30,
                width * 0.20, crownHeight * 0.34);
    }

    /**
     * Draws the outstretched arm and the pistol at the end of it.
     *
     * @param gc     the graphics context
     * @param centre the shooter's head centre X
     * @param top    the shooter's head top edge
     * @param width  the head's drawn width
     * @param height the head's drawn height
     * @param lean   how far out of the window they are, from 0 to 1
     */
    private void drawPistolArm(GraphicsContext gc, double centre, double top, double width,
            double height, double lean) {
        double shoulderX = centre + width * 0.30;
        double shoulderY = top + height * 0.72;
        double handX = centre + width * (0.9 + 2.0 * lean);
        double handY = top + height * (0.62 - 0.24 * lean);

        gc.save();
        gc.setLineWidth(Math.max(1.6, width * 0.36));
        gc.setStroke(Color.web("#16233f"));
        gc.strokeLine(shoulderX, shoulderY, handX, handY);

        // The pistol, held out level.
        gc.setFill(Color.web("#12161d"));
        double gunLength = width * 0.95;
        double gunHeight = Math.max(1.6, height * 0.10);
        gc.fillRect(handX, handY - gunHeight / 2, gunLength, gunHeight);

        if (muzzleTimer > 0) {
            double flash = muzzleTimer / (double) MUZZLE_STEPS;
            double size = width * (1.0 + 0.9 * flash);
            gc.setGlobalAlpha(flash);
            gc.setFill(Color.web("#fff3b0"));
            gc.fillOval(handX + gunLength - size * 0.25, handY - size / 2, size, size);
            gc.setFill(Color.web("#ffb347"));
            gc.fillOval(handX + gunLength - size * 0.10, handY - size * 0.3, size * 0.6,
                    size * 0.6);
        }
        gc.restore();
        gc.setGlobalAlpha(1.0);
    }

    /**
     * Blends between two numbers.
     *
     * @param from  the value at {@code blend == 0}
     * @param to    the value at {@code blend == 1}
     * @param blend how far between them
     * @return the interpolated value.
     */
    private static double lerp(double from, double to, double blend) {
        return from + (to - from) * blend;
    }

    /**
     * Acceptor method for the Visitor pattern.
     */
    @Override
    public void accept(Visitor visitor) {
        // Catching the player is handled by the game loop, not by visitation.
    }
}
