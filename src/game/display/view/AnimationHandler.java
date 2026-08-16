package game.display.view;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import game.display.models.Jaywalker;
import game.display.models.Passenger;
import game.display.models.Police;
import game.display.models.Pothole;
import game.display.models.Ride;
import game.display.models.Road;
import game.display.models.Sprite;
import game.display.models.Taxi;
import game.logic.InputHandler;
import game.logic.PassengerPool;
import game.logic.SpriteVisitor;
import game.utility.DifficultyLoader;
import game.utility.DifficultyProfile;
import game.utility.ESettings;
import game.utility.ETaxiPositions;
import game.utility.Sounds;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * This class is the defacto game, its loop is the main loop of the game.
 *
 * <p>
 * The loop runs on a fixed timestep. JavaFX calls {@link #handle(long)} once per display
 * refresh, which is 60Hz on some machines and 144Hz or more on others; stepping the
 * simulation once per call would have made the game run more than twice as fast on a
 * high-refresh monitor. Instead the elapsed time is banked and the world is advanced in
 * fixed 1/60s steps, so the game plays identically everywhere and simply draws more often
 * on a faster display.
 * </p>
 */
public class AnimationHandler extends AnimationTimer {

    // ---- Timing ----
    /** One logic step, in nanoseconds: the game's rules are written against 60 steps a second. */
    private static final long STEP_NANOS = 16_666_667L;
    /** Longest gap we will try to catch up on; beyond this the time is dropped. */
    private static final long MAX_FRAME_NANOS = 250_000_000L;
    /** Upper bound on catch-up steps per frame, so a stall cannot spiral. */
    private static final int MAX_STEPS_PER_FRAME = 5;

    // ---- Game rules ----
    /** How long a NOS boost lasts, in logic steps. */
    private static final int NOS_DURATION_STEPS = 240;
    /** Scroll speed while slowed by a pothole or a pickup. */
    private static final double SLOW_SPEED = -5;
    /** Normal scroll speed. */
    private static final double BASE_SPEED = -7;
    /** Scroll speed while NOS is firing. */
    private static final double NOS_SPEED = -20;
    /** Crawl speed while pulled over dropping fares off. */
    private static final double DROP_OFF_SPEED = -1.2;
    /** Fraction of the gap to the target speed closed per step while speeding up. */
    private static final double SPEED_EASE_UP = 0.055;
    /**
     * Fraction of the gap closed per step while slowing down.
     *
     * <p>
     * Faster than the acceleration, because a vehicle stops harder than it starts, and
     * because the drop-off wants to feel like braking rather than like coasting.
     * </p>
     */
    private static final double SPEED_EASE_DOWN = 0.11;
    /** Metres of a fare's trip covered per pixel of road scrolled. */
    private static final double METRES_PER_PIXEL = 0.1;
    /**
     * Minimum steps between two fares getting out.
     *
     * <p>
     * A held key auto-repeats far faster than a person can tap, so without a floor the
     * drop-off would be over instantly. This caps it at ten a second: fast enough that
     * mashing feels responsive, slow enough that a full taxi is a real commitment.
     * </p>
     */
    private static final int DROP_OFF_INTERVAL = 6;
    /** How much harder the police push while the taxi is stationary. */
    private static final double DROP_OFF_CHASE_MULTIPLIER = 1.25;
    /** Distance at which the police car is drawn on the road, relative to the taxi. */
    private static final int POLICE_ROAD_OFFSET = -200;
    /** Gap, in metres, below which the player is warned. */
    private static final int POLICE_WARNING_DISTANCE = 220;
    /** How long the "pedestrian down" banner stays up, in steps. */
    private static final int BANNER_STEPS = 150;
    /** How long the "you have been shot" banner stays up, in steps. */
    private static final int SHOT_BANNER_STEPS = 110;
    /** Steps of slowdown a round through a tyre costs. */
    private static final int SHOT_PUNISHMENT = 45;

    // ---- HUD layout ----
    private static final double HUD_X = 10;
    private static final double HUD_WIDTH = 250;
    private static final double INFO_PANEL_Y = 10;
    private static final double INFO_PANEL_HEIGHT = 172;
    private static final double SLIMED_PANEL_Y = 190;
    private static final double NOS_PANEL_Y = 238;

    // ---- Collaborators ----
    private final GraphicsContext gc;
    private final List<Sprite> sprites;
    private final List<Road> roads;
    private final PassengerPool passengerPool;
    private final SpriteVisitor spriteVisitor;
    private final Backdrop backdrop;
    private final Effects effects;
    private final Taxi taxi;
    private final Police police;
    private final Image passengerImage;

    private DifficultyProfile profile;

    // ---- Timing state ----
    private long previousNanos = 0;
    private long accumulatedNanos = 0;

    // ---- Run state ----
    private double speedAdjust = BASE_SPEED;
    private double roadOffset = 0;
    private int stepCount = 0;
    private int activePassengers = 0;
    private int pickupCount = 0;
    private int perfectDrops = 0;
    private int slimedCount = 0;
    private int shotsTaken = 0;
    private int shotsDodged = 0;
    private int nosCounter = 0;
    private int elapsedSteps = 0;
    private int bannerTimer = 0;
    private int shotBannerTimer = 0;
    private boolean droppingOff = false;
    private int dropCooldown = 0;
    private int deliveredThisStop = 0;
    private boolean sirenSounded = false;
    private boolean stopGame = false;
    private boolean lostGame = false;
    private boolean endNotified = false;
    private Runnable onRunEnded = null;

    // ---- Fonts ----
    private final Font hudFont = Font.font("Verdana", 16);
    private final Font hudSmallFont = Font.font("Verdana", 12);
    private final Font hudTitleFont = Font.font("Verdana", 13);
    private final Font warningFont = Font.font("Verdana", 30);
    private final Font bannerFont = Font.font("Verdana", 26);
    private final Font overlayTitleFont = Font.font("Verdana", 46);
    private final Font overlayFont = Font.font("Verdana", 19);

    /**
     * Constructor for the AnimationHandler class.
     *
     * @param gc             The GraphicsContext of the canvas.
     * @param sprites        The collidable sprites (potholes, fares and pedestrians).
     * @param roads          The scrolling road tiles.
     * @param passengerImage The sprite used for fares and pedestrians.
     */
    public AnimationHandler(GraphicsContext gc, List<Sprite> sprites, List<Road> roads,
            Image passengerImage) {
        super();
        this.gc = gc;
        this.sprites = sprites;
        this.roads = roads;
        this.passengerImage = passengerImage;
        this.taxi = InputHandler.getTaxi();
        this.police = InputHandler.getPolice();
        this.passengerPool = PassengerPool.getInstance();
        this.spriteVisitor = new SpriteVisitor();
        this.backdrop = new Backdrop();
        this.effects = new Effects();
        this.profile = DifficultyLoader.getProfile();
    }

    /**
     * Registers a callback fired once, the moment a run is won or lost.
     *
     * @param callback the action to run, typically banking the player's progress.
     */
    public void setOnRunEnded(Runnable callback) {
        this.onRunEnded = callback;
    }

    @Override
    public void start() {
        // Drop whatever time passed while the game sat on a menu, so the run does not
        // open with a burst of catch-up steps.
        previousNanos = 0;
        accumulatedNanos = 0;
        super.start();
    }

    /**
     * This function resets the game, setting all actors back to their initial states.
     */
    public void resetGame() {
        profile = DifficultyLoader.getProfile();

        stopGame = false;
        lostGame = false;
        endNotified = false;
        pickupCount = 0;
        perfectDrops = 0;
        slimedCount = 0;
        shotsTaken = 0;
        shotsDodged = 0;
        activePassengers = 0;
        stepCount = 0;
        elapsedSteps = 0;
        nosCounter = 0;
        bannerTimer = 0;
        shotBannerTimer = 0;
        droppingOff = false;
        dropCooldown = 0;
        deliveredThisStop = 0;
        sirenSounded = false;
        roadOffset = 0;
        speedAdjust = BASE_SPEED;
        previousNanos = 0;
        accumulatedNanos = 0;

        police.resetChase(profile.getPoliceStart());
        police.setOccupantImage(passengerImage);
        taxi.resetRunState();
        taxi.setOccupantImage(passengerImage);
        // scale() re-centres the taxi, so its horizontal position needs no separate reset.
        taxi.scale(ETaxiPositions.values()[InputHandler.DEFAULT_LANE]);

        spriteVisitor.getCleanList().clear();
        spriteVisitor.getBottomRenderList().clear();
        spriteVisitor.getSplatList().clear();
        backdrop.reset();
        effects.clear();

        InputHandler.resetForNewRun();
    }

    /**
     * This function is called once per displayed frame; it advances the world on a fixed
     * timestep and then draws it.
     *
     * @param now the frame timestamp supplied by JavaFX, in nanoseconds.
     */
    @Override
    public void handle(long now) {
        if (previousNanos == 0) {
            previousNanos = now;
        }
        long elapsed = Math.min(now - previousNanos, MAX_FRAME_NANOS);
        previousNanos = now;

        if (!stopGame && !InputHandler.isPaused()) {
            accumulatedNanos += elapsed;
            int steps = 0;
            while (accumulatedNanos >= STEP_NANOS && steps < MAX_STEPS_PER_FRAME) {
                update();
                accumulatedNanos -= STEP_NANOS;
                steps++;
                if (stopGame) {
                    break;
                }
            }
            if (steps == MAX_STEPS_PER_FRAME) {
                // We are behind the display; drop the debt rather than accumulate it.
                accumulatedNanos = 0;
            }
        } else {
            accumulatedNanos = 0;
            // Debris keeps settling behind the game-over card; it looks dead otherwise.
            // A pause, by contrast, should freeze everything.
            if (stopGame) {
                effects.update(0);
            }
        }

        render();

        if (stopGame && !endNotified) {
            endNotified = true;
            InputHandler.endGame();
            if (onRunEnded != null) {
                onRunEnded.run();
            }
        }
    }

    // ------------------------------------------------------------------
    // Simulation
    // ------------------------------------------------------------------

    /**
     * Advances the world by exactly one 1/60s step.
     */
    private void update() {
        collectFares();
        processCasualties();
        processPotholeHits();

        stepCount++;
        elapsedSteps++;
        if (bannerTimer > 0) {
            bannerTimer--;
        }
        if (shotBannerTimer > 0) {
            shotBannerTimer--;
        }

        applySpeed();
        applyNos();
        applyChase();

        // Every fare in the back is watching the road go past their destination.
        taxi.advanceRides(Math.abs(speedAdjust) * METRES_PER_PIXEL);
        announceReadyFares();

        backdrop.update(speedAdjust);
        scrollRoad();
        // While pulled over, SPACE belongs to the drop-off and nothing else.
        spriteVisitor.setPickupsEnabled(!droppingOff);
        moveSprites();
        // Runs after collisions, so a SPACE press that no waiting fare claimed is free to
        // mean "pull over and let this lot out" instead.
        updateDropOff();
        updatePolice();
        taxi.tickPresentation();
        effects.update(speedAdjust);
        checkEndConditions();

        // Every eight steps the cars animate. A taxi at the kerb does not roll its wheels;
        // the police very much do.
        if (stepCount % 8 == 0) {
            if (!droppingOff) {
                taxi.swapImage();
            }
            police.swapImage();
        }

        if (stepCount >= profile.getSpawnSteps()) {
            stepCount = 0;
            InputHandler.pickupBlock();
            populateStreet();
        }
    }

    /**
     * Works out this step's scroll speed.
     *
     * <p>
     * The speed is eased towards its target rather than assigned, so pulling over is a brake
     * and pulling away is an acceleration. Snapping between the two used to be the single
     * ugliest moment in the game: a taxi doing 7 pixels a step became one doing 1.2 between
     * consecutive frames, which read as the world stuttering rather than as the taxi stopping.
     * </p>
     */
    private void applySpeed() {
        boolean slowed = taxi.getPunishment() > 0;
        if (slowed) {
            taxi.minusPunishment();
        }

        double target;
        if (droppingOff) {
            // Pulled over at the kerb. Everything else is secondary to getting moving again.
            target = DROP_OFF_SPEED;
        } else if (taxi.isNosActive()) {
            target = NOS_SPEED;
        } else if (slowed) {
            target = SLOW_SPEED - taxi.getEngineUpgrade();
        } else {
            target = BASE_SPEED - taxi.getEngineUpgrade();
        }

        double ease = Math.abs(target) < Math.abs(speedAdjust) ? SPEED_EASE_DOWN : SPEED_EASE_UP;
        speedAdjust += (target - speedAdjust) * ease;
    }

    /**
     * Runs the drop-off stop: starting it, letting fares out, and ending it.
     *
     * <p>
     * This is the run's one deliberate pause. Fares pay on arrival rather than on pickup,
     * so the player is always carrying money they have not earned yet, and cashing it in
     * means standing still with the police closing. A bigger taxi is worth buying because
     * it turns several of these stops into one.
     * </p>
     */
    private void updateDropOff() {
        if (dropCooldown > 0) {
            dropCooldown--;
        }

        boolean steered = InputHandler.consumeSteering();

        if (!droppingOff) {
            if (taxi.getOccupants() == 0) {
                InputHandler.consumeDropRequest();
                return;
            }

            // Two ways in. A full taxi cannot pick anyone else up, so SPACE has nothing
            // else to mean and stopping is the obvious next move. Banking a half load is
            // a judgement call, so it gets its own key rather than firing on a stray press.
            boolean askedEarly = InputHandler.consumeDropRequest();
            boolean fullAndPressed = taxi.isFull() && InputHandler.consumeSpacePress();

            if (askedEarly || fullAndPressed) {
                droppingOff = true;
                deliveredThisStop = 0;
                dropCooldown = 0;
                taxi.nudge(1.4);
                effects.floatText("PULLING OVER", taxi.getX() + taxi.renderWidth() / 2,
                        taxi.getY() - 16, Color.web("#ffd166"), false);
            }
            return;
        }

        // A stop already under way ignores further "pull over" taps.
        InputHandler.consumeDropRequest();

        // Touching the controls means driving off with whoever is still aboard.
        if (steered) {
            endDropOff();
            return;
        }

        if (taxi.getOccupants() == 0) {
            endDropOff();
            return;
        }

        if (dropCooldown == 0 && InputHandler.consumeSpacePress()) {
            releaseOneFare();
        }

        // Standing still is expensive: this is the pressure the whole mechanic runs on.
        police.changeDistance(profile.getSlowGain() * DROP_OFF_CHASE_MULTIPLIER);
    }

    /**
     * Lets a single fare out, banking what they owe.
     *
     * <p>
     * Whoever is nearest to where they asked to go goes first, so a stop pays best in its
     * opening moments and worse the longer it runs on. That is the tension the destinations
     * were added for: the player is choosing a moment, not merely a button.
     * </p>
     */
    private void releaseOneFare() {
        Ride ride = taxi.dropOffBest();
        if (ride == null) {
            return;
        }

        Sounds.play(Sounds.Sfx.DROPOFF);

        dropCooldown = DROP_OFF_INTERVAL;
        deliveredThisStop++;
        pickupCount++;

        double taxiWidth = taxi.renderWidth();
        double taxiHeight = taxi.renderHeight();
        double doorX = taxi.getX() + taxiWidth * 0.30;
        double doorY = taxi.getY() + taxiHeight * 0.42;
        double figureHeight = taxiHeight * 0.34;
        double figureWidth = figureHeight * 0.52;

        // Out of the sliding door, down onto the kerb behind the taxi.
        effects.alight(passengerImage, doorX, doorY, figureWidth, figureHeight,
                doorX - taxiWidth * 0.20, taxi.getY() + taxiHeight * 0.86);
        // The body lifts as their weight comes off the springs.
        taxi.nudge(-1.2);

        Ride.Grade grade = ride.getGrade();
        String note;
        switch (grade) {
            case ON_THE_DOT:
                note = "ON THE DOT!";
                perfectDrops++;
                effects.flash(Color.color(0.16, 0.85, 0.40, 0.10), 5);
                break;
            case NEARLY:
                note = "CLOSE ENOUGH";
                break;
            case OVERSHOT:
                note = "LONG WAY ROUND";
                break;
            case EARLY:
            default:
                note = "NOWHERE NEAR";
                break;
        }

        effects.floatText(String.format("%s  +R%.2f", note, ride.getPayout()),
                doorX, doorY - 22, Taxi.gradeColour(grade), grade == Ride.Grade.ON_THE_DOT);
    }

    /**
     * Shouts once for each fare that has just reached where they asked to be taken.
     *
     * <p>
     * The seat lights already say it, but they are small and the player is watching the road.
     * The shout is what turns the destination system into something you can play by ear.
     * </p>
     */
    private void announceReadyFares() {
        boolean shouted = false;
        for (Ride ride : taxi.getRides()) {
            if (ride.isAnnounced() || ride.getGrade() != Ride.Grade.ON_THE_DOT) {
                continue;
            }
            ride.markAnnounced();
            // Several fares can come good on the same step; one shout covers them all.
            if (!shouted) {
                shouted = true;
                Sounds.play(Sounds.Sfx.READY);
                effects.floatText("FARE AT THEIR STOP",
                        taxi.getX() + taxi.renderWidth() / 2, taxi.getY() - 14,
                        Color.web("#3ef07a"), false);
            }
        }
    }

    /**
     * Ends the stop and gets the taxi moving again.
     */
    private void endDropOff() {
        if (!droppingOff) {
            return;
        }
        droppingOff = false;
        dropCooldown = 0;
        if (deliveredThisStop > 0) {
            effects.floatText(String.format("%d DROPPED", deliveredThisStop),
                    taxi.getX() + taxi.renderWidth() / 2, taxi.getY() - 16,
                    Color.web("#8ce99a"), false);
        }
        deliveredThisStop = 0;
    }

    /**
     * Runs the NOS boost down, if one is firing.
     */
    private void applyNos() {
        if (!taxi.isNosActive()) {
            return;
        }
        nosCounter++;
        if (nosCounter >= NOS_DURATION_STEPS) {
            nosCounter = 0;
            taxi.deactivateNos();
        }
    }

    /**
     * Moves the police, according to how well the taxi is being driven and how angry they are.
     */
    private void applyChase() {
        // Baseline drift. On Hard this is positive, so the gap closes even on a perfect run.
        police.changeDistance(profile.getCleanDrift());

        // While pulled over, updateDropOff() applies its own (larger) gain instead, so the
        // two must not stack on a taxi that stopped mid-slowdown.
        if (taxi.getPunishment() > 0 && !droppingOff) {
            police.changeDistance(profile.getSlowGain());
        }
        if (taxi.isNosActive()) {
            police.changeDistance(profile.getNosDrift());
        }
        if (police.isEnraged()) {
            police.changeDistance(profile.getEnrageGain());
            police.tickEnrage();
        }
    }

    /**
     * Scrolls the road backdrop, wrapping on the tile spacing so the seam never shows.
     */
    private void scrollRoad() {
        int spacing = ESettings.ROAD_TILE_SPACING.getVal();
        roadOffset += speedAdjust;
        while (roadOffset <= -spacing) {
            roadOffset += spacing;
        }
        // The first tile starts one step off screen to the left, so the leftmost pixel of
        // the canvas is always covered by opaque road rather than by a tile's clipped corner.
        for (int i = 0; i < roads.size(); i++) {
            roads.get(i).setX((int) Math.floor(roadOffset) + ((i - 1) * spacing));
        }
    }

    /**
     * Scrolls every sprite, lets it move under its own power, tests it against the taxi and
     * retires the ones that are finished.
     */
    private void moveSprites() {
        spriteVisitor.getBottomRenderList().clear();
        int width = ESettings.SCENE_WIDTH.getVal();
        List<Sprite> doomed = null;

        for (Sprite sprite : sprites) {
            sprite.setX((int) Math.ceil(sprite.getX() + speedAdjust));
            sprite.tick();

            if (taxi.intersects(sprite)) {
                sprite.accept(spriteVisitor);
            }

            if (sprite.wrapsAround()) {
                if (sprite.getX() < -width) {
                    sprite.setX(width);
                }
            } else if (sprite.isExpired() || sprite.getX() < -width) {
                if (doomed == null) {
                    doomed = new ArrayList<>();
                }
                doomed.add(sprite);
            }
        }

        if (doomed != null) {
            sprites.removeAll(doomed);
        }
    }

    /**
     * Drives the pursuit: where the car is, which lane it is leaning into, and what the
     * officer in the back window is doing about it.
     *
     * <p>
     * The car is re-scaled into the player's lane every step. It used to be scaled only when
     * the player pressed up or down, which meant a police car that caught up while the player
     * held a lane arrived still wearing the size and position of its mirror portrait — drawn
     * tiny at the top of the screen, with a hit box that could never touch the taxi.
     * </p>
     */
    private void updatePolice() {
        police.followLane(InputHandler.getLane());
        police.setX((int) Math.round(police.getDistance()) + POLICE_ROAD_OFFSET);

        if (police.isHidden()) {
            return;
        }

        double gap = taxi.getX() - police.getX();
        police.updateChase(InputHandler.getLane(), gap);
        police.changeDistance(police.tickSurge(gap));

        if (police.isAiming() && !sirenSounded) {
            sirenSounded = true;
            Sounds.play(Sounds.Sfx.SIREN);
        }

        resolveShot();
    }

    /**
     * Deals with a round that has just been fired.
     *
     * <p>
     * A shot locks onto a lane part way through its wind-up, so it hits the lane the taxi was
     * in rather than the lane it is in. Changing lane late is therefore always an out, which
     * is what keeps the shooting a thing to play against rather than a tax on being caught.
     * A hit costs a tyre — a slowdown, and the police close while you limp — but never the run
     * outright. Only actually being caught does that.
     * </p>
     */
    private void resolveShot() {
        Police.Shot shot = police.consumeShot();
        if (shot == Police.Shot.NONE) {
            return;
        }

        Sounds.play(Sounds.Sfx.GUNSHOT);
        double muzzleX = police.getMuzzleX();
        double muzzleY = police.getMuzzleY();

        if (shot == Police.Shot.HIT) {
            double hitX = taxi.getX() + taxi.renderWidth() * 0.16;
            double hitY = taxi.getY() + taxi.renderHeight() * 0.80;

            effects.tracer(muzzleX, muzzleY, hitX, hitY);
            effects.sparks(hitX, hitY, Color.web("#ffd166"), 24);
            effects.shake(12);
            effects.flash(Color.color(0.80, 0.04, 0.05, 0.17), 6);
            effects.floatText("TYRE SHOT!", taxi.getX() + taxi.renderWidth() / 2,
                    taxi.getY() - 18, Color.web("#ff4d4d"), true);

            taxi.addPunishment(SHOT_PUNISHMENT);
            taxi.nudge(2.4);
            shotsTaken++;
            shotBannerTimer = SHOT_BANNER_STEPS;
            return;
        }

        // The round goes through the space the taxi has just left.
        ETaxiPositions missed = ETaxiPositions.values()[clampLane(police.getLockedLane())];
        double missY = missed.getLocation() + missed.getHeightScale() * 0.82;
        double missX = taxi.getX() + taxi.renderWidth() * 0.5;

        effects.tracer(muzzleX, muzzleY, missX, missY);
        effects.sparks(missX, missY, Color.web("#ffe08a"), 16);
        effects.floatText("MISSED!", missX, missY - 26, Color.web("#4dd4ff"), false);
        shotsDodged++;
    }

    /**
     * Moves collected fares into the taxi's seats, along with where they want to go.
     */
    private void collectFares() {
        List<Passenger> collected = spriteVisitor.getCleanList();
        if (collected.isEmpty()) {
            return;
        }

        for (Passenger p : collected) {
            sprites.remove(p);
            // The money rides in the back until it is dropped off; nothing is banked here.
            taxi.board(p.getBaseFare(), p.getTripDistance());
            taxi.nudge(0.9);

            // Show them getting in, rather than simply vanishing off the pavement.
            effects.board(passengerImage, p.getX(), p.getY(), p.renderWidth(), p.renderHeight(),
                    taxi.getX() + taxi.renderWidth() * 0.42, taxi.getY() + taxi.renderHeight() * 0.44);
            Sounds.play(Sounds.Sfx.PICKUP);
            // The destination is the one thing worth knowing about a fare, so it is said out
            // loud on the pavement rather than buried in the HUD.
            effects.floatText(String.format("GOING %.0fm", p.getTripDistance()),
                    p.getX() + p.renderWidth() / 2, p.getY() - 10, Color.web("#4dd4ff"), false);

            passengerPool.releasePassenger(p);
            activePassengers--;
        }
        collected.clear();

        // Announce the moment the last seat goes, once.
        if (taxi.isFull()) {
            Sounds.play(Sounds.Sfx.FULL);
        }
    }

    /**
     * Handles pedestrians the taxi has just run down.
     */
    private void processCasualties() {
        List<Jaywalker> hit = spriteVisitor.getSplatList();
        if (hit.isEmpty()) {
            return;
        }

        for (Jaywalker jw : hit) {
            slimedCount++;
            taxi.addSlimed();

            double height = jw.currentHeight();
            effects.splat(jw.getX() + jw.renderWidth() / 2, jw.getY() + height * 0.55, height);
            // And a good deal of them ends up on the front of the taxi, where it stays.
            taxi.splatterFront();
            Sounds.play(Sounds.Sfx.SPLAT);
            effects.floatText("SPLAT!", jw.getX() + jw.renderWidth() / 2, jw.getY() - 14,
                    Color.web("#ff4d4d"), true);

            // Witnesses. The police close a chunk of the gap and drive angry for a while.
            police.changeDistance(profile.getSplatPenalty());
            police.enrage(profile.getEnrageSteps());
            bannerTimer = BANNER_STEPS;
        }
        hit.clear();
    }

    /**
     * Gives the player a jolt for each pothole struck.
     */
    private void processPotholeHits() {
        List<Pothole> hits = spriteVisitor.getPotholeHits();
        if (hits.isEmpty()) {
            return;
        }
        for (Pothole hole : hits) {
            Sounds.play(Sounds.Sfx.POTHOLE);
            effects.shake(7);
            taxi.nudge(2.0);
            effects.floatText("THUD", hole.getX() + 20, hole.getY() - 6, Color.web("#ffd166"), false);
        }
        hits.clear();
    }

    /**
     * Puts someone new on the street: usually a fare, sometimes somebody crossing.
     */
    private void populateStreet() {
        int width = ESettings.SCENE_WIDTH.getVal();

        if (ThreadLocalRandom.current().nextDouble() < profile.getJaywalkerChance()
                && countJaywalkers() < profile.getMaxJaywalkers()) {
            Jaywalker jw = new Jaywalker(
                    ThreadLocalRandom.current().nextInt(width, width + 520),
                    ThreadLocalRandom.current().nextBoolean());
            jw.setImage(passengerImage);
            sprites.add(jw);
            return;
        }

        if (activePassengers < profile.getMaxFares()) {
            sprites.add(passengerPool.acquirePassenger());
            activePassengers++;
        }
    }

    /**
     * Counts how many people are currently in the road.
     *
     * <p>
     * Capped so that a high crossing rate makes the road feel dangerous rather than
     * impassable — without a limit, Hard fills every lane and leaves the player nowhere
     * to go.
     * </p>
     *
     * @return the number of live pedestrians.
     */
    private int countJaywalkers() {
        int count = 0;
        for (Sprite sprite : sprites) {
            if (sprite instanceof Jaywalker jw && !jw.isSplatted()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Ends the run once the police make contact or the target is reached.
     */
    private void checkEndConditions() {
        if (!police.isHidden() && taxi.intersects(police)) {
            stopGame = true;
            lostGame = true;
            effects.shake(18);
            effects.flash(Color.color(0.6, 0.05, 0.10, 0.35), 12);
            Sounds.play(Sounds.Sfx.BUST);
            return;
        }
        if (pickupCount >= ESettings.TARGET_PASSENGERS.getVal()) {
            stopGame = true;
            lostGame = false;
            Sounds.play(Sounds.Sfx.WIN);
        }
    }

    /**
     * Keeps a lane index inside the four road lanes.
     *
     * @param lane the raw index
     * @return the index clamped to a real lane.
     */
    private static int clampLane(int lane) {
        return Math.min(ETaxiPositions.values().length - 1, Math.max(0, lane));
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    /**
     * Draws the current state of the world.
     */
    private void render() {
        // The world shakes on impact; the HUD deliberately does not, so it stays readable.
        gc.save();
        gc.translate(effects.getShakeX(), effects.getShakeY());

        backdrop.render(gc);
        for (Road road : roads) {
            road.draw(gc);
        }
        // The near verge goes over the road sprite, so its grass breaks the hard kerb line.
        backdrop.renderForeground(gc, ESettings.ROAD_Y.getVal() + roadHeight());
        // Stains lie on the tarmac, so they go under everything that drives over them.
        effects.renderGround(gc);

        List<Passenger> foreground = spriteVisitor.getBottomRenderList();
        for (Sprite sprite : sprites) {
            if (!foreground.contains(sprite)) {
                sprite.draw(gc);
            }
        }

        // No shadow is drawn under either vehicle: both sprites are supplied with one baked
        // in, and a second ellipse underneath only ever showed as a smudge around the first.
        taxi.draw(gc);

        if (!police.isHidden()) {
            police.draw(gc);
        }

        // Passengers on the near pavement stand in front of the taxi.
        for (Passenger p : foreground) {
            p.draw(gc);
        }

        effects.renderWorld(gc);
        drawAimLine();
        if (taxi.isNosActive()) {
            drawSpeedLines();
        }
        effects.renderText(gc);

        gc.restore();

        effects.renderFlash(gc);
        drawDangerVignette();
        renderHud();

        if (stopGame) {
            renderEndOverlay();
        } else if (InputHandler.isPaused()) {
            renderPauseOverlay();
        }
    }

    /**
     * Returns the drawn height of the road strip.
     *
     * @return the road sprite's height, or a sensible default before assets are attached.
     */
    private double roadHeight() {
        return roads.isEmpty() ? 269 : roads.get(0).renderHeight();
    }

    /**
     * Draws the officer's aim: a line onto the lane they have picked, and a reticle that
     * closes as they settle on it.
     *
     * <p>
     * Drawn onto the lane rather than onto the taxi on purpose. The whole point is that it is
     * the <em>lane</em> being aimed at, so a player who reads the line can see the shot is
     * still coming to where they are standing and get out of it.
     * </p>
     */
    private void drawAimLine() {
        if (!police.isAiming()) {
            return;
        }
        double progress = police.getAimProgress();
        if (progress < 0.12) {
            return;
        }

        ETaxiPositions lane = ETaxiPositions.values()[clampLane(police.getLockedLane())];
        double targetX = taxi.getX() + taxi.renderWidth() * 0.30;
        double targetY = lane.getLocation() + lane.getHeightScale() * 0.82;

        // The hand is unsteady until the aim settles, so the line wanders and then stops.
        boolean locked = police.isLockedOn();
        double wobble = locked ? 0 : (1.0 - progress) * 22 * Math.sin(elapsedSteps * 0.9);
        Color colour = locked ? Color.web("#ff2d2d") : Color.web("#ffa94d");

        gc.save();
        gc.setGlobalAlpha(0.20 + 0.55 * progress);
        gc.setStroke(colour);
        gc.setLineWidth(locked ? 2.2 : 1.4);
        gc.strokeLine(police.getMuzzleX(), police.getMuzzleY(), targetX, targetY + wobble);

        double radius = 34 - 22 * progress;
        gc.setLineWidth(2);
        gc.strokeOval(targetX - radius, targetY + wobble - radius, radius * 2, radius * 2);
        gc.strokeLine(targetX - radius * 1.5, targetY + wobble, targetX - radius * 0.6,
                targetY + wobble);
        gc.strokeLine(targetX + radius * 0.6, targetY + wobble, targetX + radius * 1.5,
                targetY + wobble);
        gc.restore();
        gc.setLineWidth(1);
        gc.setGlobalAlpha(1.0);
    }

    /**
     * Streaks the screen while NOS is firing.
     */
    private void drawSpeedLines() {
        double width = ESettings.SCENE_WIDTH.getVal();
        gc.setStroke(Color.color(1, 1, 1, 0.5));
        gc.setLineWidth(2);
        for (int i = 0; i < 16; i++) {
            // Seeded from the step counter so the streaks flicker rather than crawl.
            double y = 370 + ((i * 137 + elapsedSteps * 13) % 250);
            double x = (elapsedSteps * 37 + i * 211) % (int) width;
            gc.strokeLine(x, y, x + 90, y);
        }
        gc.setLineWidth(1);
    }

    /**
     * Washes the edges of the screen as the police close in.
     *
     * <p>
     * Red until they are actually visible, and then alternating red and blue in time with
     * their light bar, so the edge of the screen tells the player where the chase is without
     * their having to look away from the road.
     * </p>
     */
    private void drawDangerVignette() {
        double intensity = police.getCloseness();
        if (police.isEnraged()) {
            intensity = Math.max(intensity, 0.55 + 0.25 * Math.sin(elapsedSteps * 0.25));
        }
        if (intensity < 0.55) {
            return;
        }

        double strength = (intensity - 0.55) / 0.45;
        double width = ESettings.SCENE_WIDTH.getVal();
        double height = ESettings.SCENE_HEIGHT.getVal();
        double band = 120;

        Color wash = Color.color(0.75, 0.02, 0.05, 0.30 * strength);
        if (!police.isHidden() && (elapsedSteps % 34) < 17) {
            wash = Color.color(0.06, 0.20, 0.85, 0.30 * strength);
        }

        gc.setFill(wash);
        gc.fillRect(0, 0, band, height);
        gc.fillRect(width - band, 0, band, height);
        gc.fillRect(0, 0, width, band * 0.5);
        gc.fillRect(0, height - band * 0.5, width, band * 0.5);
    }

    /**
     * Draws the heads-up display: earnings, progress and the state of the chase.
     */
    private void renderHud() {
        gc.setTextAlign(TextAlignment.LEFT);

        gc.setFill(Color.color(0, 0, 0, 0.58));
        gc.fillRoundRect(HUD_X, INFO_PANEL_Y, HUD_WIDTH, INFO_PANEL_HEIGHT, 14, 14);

        gc.setFont(hudTitleFont);
        gc.setFill(Color.web("#ffd166"));
        gc.fillText("TAXI INFORMATION", 24, 32);

        gc.setFont(hudFont);
        gc.setFill(Color.web("#8ce99a"));
        gc.fillText(String.format("Banked   R%.2f", taxi.getWallet()), 24, 58);

        // The money in the back is the whole tension: it is not yours until you stop, and it
        // is worth what stopping right now would pay, not what the meter once read.
        double unbanked = taxi.getUnbankedTotal();
        gc.setFill(unbanked > 0 ? Color.web("#ffd166") : Color.web("#6d7887"));
        gc.fillText(String.format("In back  R%.2f", unbanked), 24, 82);

        gc.setFill(Color.WHITE);
        gc.fillText(String.format("Delivered %d / %d", pickupCount,
                ESettings.TARGET_PASSENGERS.getVal()), 24, 106);

        renderLoadGauge(24, 116, 212, 20);
        renderReadyLine();

        renderSlimedCounter();
        renderNosIndicator();
        renderChaseStatus();
        renderBanner();
        renderDropOffPrompt();
    }

    /**
     * Draws one pip per seat: filled from the bottom as that fare's trip is driven, and
     * coloured by how close they are to where they asked to go.
     *
     * <p>
     * This is the destination system's only permanent readout, so it carries both facts at
     * once — how far along each trip is, and whether it is worth money yet.
     * </p>
     *
     * @param x      left edge
     * @param y      top edge
     * @param width  total width available
     * @param height pip height
     */
    private void renderLoadGauge(double x, double y, double width, double height) {
        int capacity = taxi.getCapacity();
        List<Ride> rides = taxi.getRides();

        double gap = 3;
        double pip = Math.max(3, (width - gap * (capacity - 1)) / capacity);
        boolean flash = (elapsedSteps % 40) < 20;

        for (int seat = 0; seat < capacity; seat++) {
            double px = x + seat * (pip + gap);

            gc.setFill(Color.color(1, 1, 1, 0.14));
            gc.fillRoundRect(px, y, pip, height, 3, 3);

            if (seat >= rides.size()) {
                continue;
            }

            Ride ride = rides.get(seat);
            Ride.Grade grade = ride.getGrade();
            // A sliver even at the start, so an occupied seat never reads as an empty one.
            double fill = Math.max(0.18, ride.getFill());
            double fillHeight = height * fill;

            gc.setGlobalAlpha(grade == Ride.Grade.ON_THE_DOT && flash ? 1.0 : 0.82);
            gc.setFill(Taxi.gradeColour(grade));
            gc.fillRoundRect(px, y + height - fillHeight, pip, fillHeight, 3, 3);
            gc.setGlobalAlpha(1.0);
        }
    }

    /**
     * Says, in words, how many fares would pay a bonus if the taxi stopped now.
     */
    private void renderReadyLine() {
        int ready = taxi.getReadyCount();
        gc.setFont(hudSmallFont);

        if (ready > 0) {
            gc.setFill(Color.web("#3ef07a"));
            gc.fillText(String.format("%d at their stop  -  worth pulling over", ready), 24, 154);
            return;
        }

        gc.setFill(Color.web("#6d7887"));
        gc.fillText(taxi.getOccupants() == 0
                ? "No fares aboard."
                : "Nobody is at their stop yet.", 24, 154);
    }

    /**
     * Tells the player when to stop, and shows the stop in progress.
     */
    private void renderDropOffPrompt() {
        double width = ESettings.SCENE_WIDTH.getVal();
        double centreX = width / 2.0;

        if (droppingOff) {
            double pulse = 0.5 + 0.5 * Math.sin(elapsedSteps * 0.5);
            gc.setFill(Color.color(0.04, 0.22 + 0.10 * pulse, 0.11, 0.90));
            gc.fillRoundRect(centreX - 285, 20, 570, 78, 14, 14);

            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(bannerFont);
            gc.setFill(Color.web("#8ce99a"));
            gc.fillText("MASH SPACE TO DROP OFF", centreX, 54);

            gc.setFont(hudFont);
            gc.setFill(Color.WHITE);
            gc.fillText(String.format("%d aboard, %d at their stop  -  change lane to pull away",
                    taxi.getOccupants(), taxi.getReadyCount()), centreX, 82);
            gc.setTextAlign(TextAlignment.LEFT);
            return;
        }

        if (taxi.isFull()) {
            boolean bright = (elapsedSteps % 50) < 25;
            gc.setFill(Color.color(0.32, 0.05, 0.02, 0.88));
            gc.fillRoundRect(centreX - 285, 20, 570, 46, 14, 14);

            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(bannerFont);
            gc.setFill(bright ? Color.web("#ff8080") : Color.web("#ffd166"));
            gc.fillText("TAXI FULL  -  PRESS SPACE TO DROP OFF", centreX, 52);
            gc.setTextAlign(TextAlignment.LEFT);
            return;
        }

        // A quiet nudge that a half load can be banked early, once there is enough in the
        // back for that to be worth doing.
        if (taxi.getOccupants() >= 2) {
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(hudSmallFont);
            gc.setFill(Color.color(1, 1, 1, 0.5));
            gc.fillText("D - pull over and bank early", centreX, 32);
            gc.setTextAlign(TextAlignment.LEFT);
        }
    }

    /**
     * Draws the body count, which stays quiet until the player earns it.
     */
    private void renderSlimedCounter() {
        boolean any = slimedCount > 0;

        gc.setFill(any ? Color.color(0.45, 0.02, 0.04, 0.82) : Color.color(0, 0, 0, 0.45));
        gc.fillRoundRect(HUD_X, SLIMED_PANEL_Y, HUD_WIDTH, 40, 12, 12);

        gc.setFont(hudFont);
        gc.setFill(any ? Color.web("#ffb3b3") : Color.web("#7c8797"));
        gc.fillText(String.format("Slimed   %d", slimedCount), 24, SLIMED_PANEL_Y + 26);

        if (any) {
            // A row of marks, one per body, so the tally reads at a glance.
            int marks = Math.min(slimedCount, 12);
            gc.setFill(Color.web("#ff4d4d"));
            for (int i = 0; i < marks; i++) {
                gc.fillOval(140 + i * 9, SLIMED_PANEL_Y + 15, 6, 6);
            }
            if (slimedCount > marks) {
                gc.setFont(hudSmallFont);
                gc.fillText("+", 140 + marks * 9, SLIMED_PANEL_Y + 22);
            }
        }
    }

    /**
     * Shows whether a NOS canister is armed, firing, or spent.
     */
    private void renderNosIndicator() {
        String label;
        Color colour;
        double fill = 0;

        if (taxi.isNosActive()) {
            fill = 1.0 - (nosCounter / (double) NOS_DURATION_STEPS);
            label = String.format("NOS FIRING  %d%%", (int) Math.round(fill * 100));
            colour = Color.web("#ff6b6b");
        } else if (taxi.hasNOS()) {
            label = "NOS READY  [E]";
            colour = Color.web("#4dd4ff");
        } else {
            return;
        }

        gc.setFill(Color.color(0, 0, 0, 0.58));
        gc.fillRoundRect(HUD_X, NOS_PANEL_Y, HUD_WIDTH, taxi.isNosActive() ? 48 : 34, 12, 12);
        gc.setFont(hudFont);
        gc.setFill(colour);
        gc.fillText(label, 24, NOS_PANEL_Y + 23);

        if (taxi.isNosActive()) {
            gc.setFill(Color.color(1, 1, 1, 0.18));
            gc.fillRoundRect(24, NOS_PANEL_Y + 31, 212, 8, 8, 8);
            gc.setFill(colour);
            gc.fillRoundRect(24, NOS_PANEL_Y + 31, Math.max(8, 212 * fill), 8, 8, 8);
        }
    }

    /**
     * Draws the rear-view panel: how far back the police are, how angry, and whether one of
     * them currently has a pistol out of the window.
     */
    private void renderChaseStatus() {
        double panelY = NOS_PANEL_Y + (taxi.isNosActive() ? 60 : (taxi.hasNOS() ? 46 : 0));
        boolean enraged = police.isEnraged();

        if (!police.isHidden()) {
            // Pulse hard once they are actually alongside.
            double pulse = 0.5 + 0.5 * Math.sin(elapsedSteps * 0.4);
            gc.setFill(Color.color(0.5 + 0.3 * pulse, 0, 0, 0.85));
            gc.fillRoundRect(HUD_X, panelY, HUD_WIDTH, 66, 12, 12);
            gc.setFont(hudFont);
            gc.setFill(Color.WHITE);
            gc.fillText(police.isSurging() ? "POLICE FLOORING IT!" : "POLICE ALONGSIDE!",
                    24, panelY + 28);

            gc.setFont(hudSmallFont);
            if (police.isAiming()) {
                gc.setFill(police.isLockedOn() ? Color.web("#ffd166") : Color.web("#ffb3b3"));
                gc.fillText(police.isLockedOn()
                        ? "Aim locked - SWERVE NOW"
                        : "An officer is leaning out...", 24, panelY + 50);
            } else {
                gc.setFill(Color.web("#ffb3b3"));
                gc.fillText("Lose them or you are done.", 24, panelY + 50);
            }
            return;
        }

        int metres = police.getMetresBehind();

        gc.setFill(enraged
                ? Color.color(0.42, 0.02, 0.04, 0.85)
                : Color.color(0, 0, 0, 0.58));
        gc.fillRoundRect(HUD_X, panelY, HUD_WIDTH, 108, 12, 12);

        // The rear-view mirror. Drawn straight from the image so the live police car keeps
        // the lane scale it needs for collisions.
        Image mirror = police.getImage();
        if (mirror != null) {
            double mirrorWidth = 96;
            gc.drawImage(mirror, 24, panelY + 14, mirrorWidth,
                    mirrorWidth * (mirror.getHeight() / mirror.getWidth()));
        }

        gc.setFont(hudTitleFont);
        gc.setFill(enraged ? Color.web("#ff8080") : Color.web("#ffd166"));
        gc.fillText(enraged ? "IN PURSUIT" : "REAR VIEW", 136, panelY + 28);

        gc.setFont(hudFont);
        gc.setFill(Color.WHITE);
        gc.fillText(String.format("%dm behind", metres), 136, panelY + 52);

        renderProximityBar(24, panelY + 84, 212, 12);

        if (metres <= POLICE_WARNING_DISTANCE) {
            renderClosingWarning(metres);
        }
    }

    /**
     * Draws a bar that fills as the police close the gap.
     *
     * @param x      left edge of the bar
     * @param y      top edge of the bar
     * @param width  bar width
     * @param height bar height
     */
    private void renderProximityBar(double x, double y, double width, double height) {
        double closeness = police.getCloseness();

        gc.setFill(Color.color(1, 1, 1, 0.18));
        gc.fillRoundRect(x, y, width, height, height, height);

        Color barColour;
        if (closeness > 0.85) {
            barColour = Color.web("#ff4d4d");
        } else if (closeness > 0.6) {
            barColour = Color.web("#ffa94d");
        } else {
            barColour = Color.web("#8ce99a");
        }
        gc.setFill(barColour);
        gc.fillRoundRect(x, y, Math.max(height, width * closeness), height, height, height);

        if (police.isEnraged()) {
            // A brighter cap showing how much of the fury is left to ride out.
            gc.setFill(Color.color(1, 1, 1, 0.55));
            gc.fillRoundRect(x, y - 5, width * police.getEnrageFraction(), 3, 3, 3);
        }
    }

    /**
     * Flashes a warning when the police are nearly on top of the player.
     *
     * <p>
     * Stands down while a shot is being lined up: that warning is more urgent, is about the
     * same police car, and needs the same strip of screen.
     * </p>
     *
     * @param metres how far behind the police are
     */
    private void renderClosingWarning(int metres) {
        if (police.isAiming()) {
            return;
        }
        // Pulse roughly twice a second so it reads as urgent without becoming unreadable.
        boolean bright = (elapsedSteps % 60) < 30;
        gc.setFont(warningFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(bright ? Color.web("#ff2d2d") : Color.web("#ffd166"));
        // Sits below the drop-off prompts, which own the top of the screen.
        gc.fillText(String.format("POLICE %dm AWAY - GO!", metres),
                ESettings.SCENE_WIDTH.getVal() / 2.0, 250);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    /**
     * Drops a banner across the screen when the player has just flattened somebody, or when
     * an officer has just put a round through a tyre.
     */
    private void renderBanner() {
        renderAimWarning();

        if (bannerTimer > 0) {
            drawBottomBanner("PEDESTRIAN DOWN  -  POLICE ENRAGED", bannerTimer,
                    0.45 + 0.25 * (0.5 + 0.5 * Math.sin(elapsedSteps * 0.35)));
        } else if (shotBannerTimer > 0) {
            drawBottomBanner("TYRE SHOT OUT  -  YOU ARE LIMPING", shotBannerTimer, 0.42);
        }
    }

    /**
     * Warns the player, in the one place they are already looking, that a shot is coming and
     * which way out of it there is.
     */
    private void renderAimWarning() {
        if (!police.isAiming() || police.getAimProgress() < 0.15) {
            return;
        }

        boolean locked = police.isLockedOn();
        boolean bright = (elapsedSteps % 20) < 10;
        double centreX = ESettings.SCENE_WIDTH.getVal() / 2.0;

        gc.setFont(warningFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(locked && bright ? Color.web("#ff2d2d") : Color.web("#ffd166"));
        gc.fillText(locked ? "SWERVE!" : "OFFICER TAKING AIM", centreX, 250);

        // A bar draining towards the shot, so the timing of the dodge is readable rather
        // than guessed at.
        double barWidth = 260;
        gc.setFill(Color.color(0, 0, 0, 0.45));
        gc.fillRoundRect(centreX - barWidth / 2, 262, barWidth, 9, 9, 9);
        gc.setFill(locked ? Color.web("#ff2d2d") : Color.web("#ffa94d"));
        gc.fillRoundRect(centreX - barWidth / 2, 262,
                Math.max(9, barWidth * police.getAimProgress()), 9, 9, 9);

        gc.setTextAlign(TextAlignment.LEFT);
    }

    /**
     * Draws a full-width banner along the bottom of the screen.
     *
     * @param message the text
     * @param timer   how many steps the banner has left, which drives its fade
     * @param red     how red the backing is
     */
    private void drawBottomBanner(String message, int timer, double red) {
        double width = ESettings.SCENE_WIDTH.getVal();
        double height = ESettings.SCENE_HEIGHT.getVal();
        double fade = Math.min(1.0, timer / 40.0);
        // Along the bottom: the left column of the HUD is already busy, and this is the one
        // strip of the screen with nothing in it.
        double bannerY = height - 86;

        gc.save();
        gc.setGlobalAlpha(fade);
        gc.setFill(Color.color(red, 0.02, 0.05, 0.92));
        gc.fillRect(0, bannerY, width, 54);
        gc.setFill(Color.color(1, 0.35, 0.35, 0.8));
        gc.fillRect(0, bannerY, width, 3);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(bannerFont);
        gc.setFill(Color.WHITE);
        gc.fillText(message, width / 2, bannerY + 36);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.restore();
    }

    /**
     * Draws the win or lose card at the end of a run.
     */
    private void renderEndOverlay() {
        String bodies = slimedCount == 0
                ? "You did not hit a soul. Respectable."
                : String.format("You flattened %d pedestrian%s. They will remember.",
                        slimedCount, slimedCount == 1 ? "" : "s");

        String accuracy = pickupCount == 0
                ? "You did not deliver anybody at all."
                : String.format("%d of %d dropped right where they asked. %s",
                        perfectDrops, pickupCount,
                        perfectDrops * 2 >= pickupCount
                                ? "Good driving."
                                : "Watch the seat lights.");

        String gunfire = shotsTaken + shotsDodged == 0
                ? "They never got close enough for a shot."
                : String.format("They fired %d time%s; you swerved out of %d.",
                        shotsTaken + shotsDodged, shotsTaken + shotsDodged == 1 ? "" : "s",
                        shotsDodged);

        // Anyone still aboard never paid, so say so plainly - it is the lesson of the run.
        double lost = taxi.getUnbankedTotal();
        String unbanked = lost > 0
                ? String.format("R%.2f drove off with %d fare%s still in the back.",
                        lost, taxi.getOccupants(), taxi.getOccupants() == 1 ? "" : "s")
                : "Nothing left unbanked. Well judged.";

        if (lostGame) {
            drawOverlay("BUSTED!", Color.web("#ff6b6b"), new String[] {
                    String.format("You delivered %d fares and banked R%.2f.",
                            pickupCount, taxi.getWallet()),
                    unbanked,
                    accuracy,
                    gunfire,
                    bodies,
                    "",
                    "Drop fares off when their light turns green, and change lane",
                    "the moment an officer draws.",
                    "",
                    "ENTER - back to the garage        R - run it again"
            });
        } else {
            drawOverlay("YOU WIN!", Color.web("#8ce99a"), new String[] {
                    String.format("%d fares delivered and R%.2f banked, and the police",
                            pickupCount, taxi.getWallet()),
                    "never laid a hand on you. That is a perfect shift.",
                    accuracy,
                    gunfire,
                    bodies,
                    "",
                    "ENTER - back to the garage        R - run it again"
            });
        }
    }

    /**
     * Draws the paused card.
     */
    private void renderPauseOverlay() {
        List<String> lines = new ArrayList<>(List.of(
                "P - resume            R - restart run",
                "ESC - back to the garage (banked cash is kept)",
                "",
                "UP / DOWN change lane.  E fires NOS.",
                "SPACE picks up a fare; once full, SPACE drops them off.",
                "D pulls over early to bank a half load."));

        // Only offered on a build that actually has sound files in res/sound.
        if (Sounds.hasAudio()) {
            lines.add(Sounds.isMuted() ? "M - unmute" : "M - mute");
        }

        lines.addAll(List.of(
                "",
                "Every fare is going somewhere. Their seat light turns green when",
                "you reach it - drop them there and they pay well over the meter.",
                "Stop short, or drive them miles past it, and they pay far less.",
                "",
                "Mind the people crossing. The police take it personally, and once",
                "they are close enough an officer will lean out and shoot at your",
                "tyres. Change lane while they are aiming and the shot goes wide."));

        drawOverlay("PAUSED", Color.WHITE, lines.toArray(new String[0]));
    }

    /**
     * Draws a dimmed, centred card with a heading and body lines.
     *
     * @param title       the heading
     * @param titleColour the heading colour
     * @param lines       body lines, top to bottom
     */
    private void drawOverlay(String title, Color titleColour, String[] lines) {
        double width = ESettings.SCENE_WIDTH.getVal();
        double height = ESettings.SCENE_HEIGHT.getVal();
        double centreX = width / 2.0;

        gc.setFill(Color.color(0, 0, 0, 0.82));
        gc.fillRect(0, 0, width, height);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(overlayTitleFont);
        gc.setFill(titleColour);
        gc.fillText(title, centreX, height / 2.0 - 90);

        gc.setFont(overlayFont);
        gc.setFill(Color.WHITE);
        // Long cards (the controls list) are set tighter so the last line still lands on
        // screen; short ones keep the roomier spacing they read best at.
        double spacing = lines.length > 11 ? 24 : 29;
        double y = height / 2.0 - 40;
        for (String line : lines) {
            gc.fillText(line, centreX, y);
            y += spacing;
        }
        gc.setTextAlign(TextAlignment.LEFT);
    }
}
