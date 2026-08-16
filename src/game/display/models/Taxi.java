package game.display.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import game.logic.Visitor;
import game.utility.ESettings;
import game.utility.ETaxiPositions;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * The player's taxi.
 *
 * <p>
 * A taxi owns two kinds of state. Career state (wallet, passengers, upgrades, whether
 * NOS has been bought) survives across runs and is written to the save file. Run state
 * (the slowdown clock, whether NOS is currently firing, who is in the back) belongs to a
 * single attempt and is cleared by {@link #resetRunState()} whenever a run ends, so it can
 * never leak into the next run or into a save file.
 * </p>
 */
public class Taxi extends Sprite {

    /** Upgrades are bought in three steps, from 0/3 up to 3/3. */
    public static final int MAX_UPGRADE = 3;

    /**
     * How many fares fit in the taxi at each capacity level.
     *
     * <p>
     * Capacity is what makes the drop-off loop breathe: a bigger taxi means fewer stops,
     * and every stop is time spent stationary while the police close.
     * </p>
     */
    private static final int[] CAPACITY_BY_LEVEL = { 5, 8, 11, 15 };

    // The window band of the taxi sprite, as fractions of the drawn image. Fractions rather
    // than pixels because the taxi is drawn at four different sizes, one per lane.
    private static final double SEAT_BAND_X = 0.175;
    private static final double SEAT_BAND_WIDTH = 0.455;
    private static final double SEAT_TOP_Y = 0.350;
    private static final double SEAT_HEIGHT = 0.105;
    /** Widest a single head is drawn, as a fraction of the taxi. Keeps a near-empty taxi sane. */
    private static final double SEAT_MAX_WIDTH = 0.062;
    /** Fraction of the passenger sprite that is head and shoulders. */
    private static final double PASSENGER_HEAD_FRACTION = 0.56;

    // ---- Mess left on the bodywork after running somebody down ----
    /** How long a splatter stays on the paint, in steps: five seconds. */
    private static final int GORE_LIFE_STEPS = 300;
    /** Steps of that life spent fading out, so it does not simply blink away. */
    private static final int GORE_FADE_STEPS = 110;
    /** Blobs thrown onto the bodywork per pedestrian. */
    private static final int GORE_SPOTS = 16;
    /** Most splatters kept at once; beyond this the oldest is dropped. */
    private static final int MAX_GORE = 48;

    // ---- Suspension ----
    /** How hard the body springs back towards level. */
    private static final double BOB_STIFFNESS = 0.22;
    /** How quickly the bounce dies away. */
    private static final double BOB_DAMPING = 0.26;

    // ---- Career state: persisted in the save file ----
    private double wallet = 0;
    private int career_passengers = 0;
    private int career_slimed = 0;
    private int engine_upgrade = 0;
    private int pothole_resistance = 0;
    private int capacity_upgrade = 0;
    private boolean hasNOS = false;

    // ---- Run state: reset at the start/end of every run ----
    private int punishment_clock = 0;
    private boolean nosActive = false;
    /**
     * Fares currently aboard, in the order they were picked up.
     *
     * <p>
     * The money is not the player's until each one is dropped off, which is the whole point
     * of the drop-off stop: a taxi full of unbanked fares is a taxi with a lot to lose. Each
     * one also carries the destination they asked for, so what they eventually pay depends
     * on where they are let out rather than only on whether they were let out.
     * </p>
     */
    private final List<Ride> onBoard = new ArrayList<>();

    // ---- Presentation state ----
    private boolean isPrime = true;
    private Image primeImage = null;
    private Image secondImage = null;
    private Image occupantImage = null;
    private ETaxiPositions myPosition = null;
    private final List<GoreSpot> gore = new ArrayList<>();
    private double bob = 0;
    private double bobVelocity = 0;
    private int pulse = 0;

    /**
     * Constructor for the Taxi class
     *
     * @param x X coordinate of the taxi
     * @param y Y coordinate of the taxi
     */
    public Taxi(int x, int y) {
        super(x, y);
    }

    /**
     * Clears everything that belongs to a single run.
     *
     * <p>
     * This is what keeps a run from contaminating the player's save: without it a taxi
     * that was mid-NOS or mid-slowdown when the police caught it would start the next
     * run already boosted, and would be written to disk in that temporary state.
     * </p>
     */
    public void resetRunState() {
        this.punishment_clock = 0;
        this.nosActive = false;
        // Anyone still aboard when a run ends never paid; their fares are not banked.
        this.onBoard.clear();
        this.gore.clear();
        this.bob = 0;
        this.bobVelocity = 0;
        this.pulse = 0;
    }

    /**
     * Supplies the sprite drawn in the taxi's windows for each fare on board.
     *
     * @param image the passenger sprite
     */
    public void setOccupantImage(Image image) {
        this.occupantImage = image;
    }

    // ------------------------------------------------------------------
    // Fares
    // ------------------------------------------------------------------

    /**
     * Takes a fare aboard, if there is a seat free.
     *
     * @param baseFare       what the meter reads for the trip they asked for
     * @param tripDistance   how far down the road they want to go, in metres
     * @return {@code true} if they got in, {@code false} if the taxi is full.
     */
    public boolean board(double baseFare, double tripDistance) {
        if (isFull()) {
            return false;
        }
        onBoard.add(new Ride(baseFare, tripDistance));
        return true;
    }

    /**
     * Runs every meter in the back on by one step's worth of road.
     *
     * @param metres how far the taxi travelled this step
     */
    public void advanceRides(double metres) {
        for (Ride ride : onBoard) {
            ride.advance(metres);
        }
    }

    /**
     * Drops off whichever fare is closest to where they asked to go, and banks what they pay.
     *
     * <p>
     * The player decides <em>when</em> to pull over, not which door opens first; picking the
     * best-paying fare here is what makes that single decision meaningful. It also means a
     * long stop degrades naturally — the fares who were ready leave first, and everyone after
     * them is being let out further from their destination than they wanted.
     * </p>
     *
     * @return the fare that got out, or {@code null} if the taxi was already empty.
     */
    public Ride dropOffBest() {
        if (onBoard.isEmpty()) {
            return null;
        }

        int best = 0;
        double bestMultiplier = onBoard.get(0).getMultiplier();
        for (int i = 1; i < onBoard.size(); i++) {
            double multiplier = onBoard.get(i).getMultiplier();
            if (multiplier > bestMultiplier) {
                bestMultiplier = multiplier;
                best = i;
            }
        }

        Ride ride = onBoard.remove(best);
        wallet += ride.getPayout();
        career_passengers++;
        return ride;
    }

    /**
     * Returns the fares currently riding along, for the load gauge and the seat lights.
     *
     * @return an unmodifiable view of the fares on board, in boarding order.
     */
    public List<Ride> getRides() {
        return Collections.unmodifiableList(onBoard);
    }

    /**
     * Returns how many fares are riding along.
     *
     * @return the number of fares currently aboard.
     */
    public int getOccupants() {
        return onBoard.size();
    }

    /**
     * Returns how many fares are in their paying sweet spot right now.
     *
     * @return the number of fares who would pay a bonus if let out this instant.
     */
    public int getReadyCount() {
        int ready = 0;
        for (Ride ride : onBoard) {
            if (ride.getGrade() == Ride.Grade.ON_THE_DOT) {
                ready++;
            }
        }
        return ready;
    }

    /**
     * Returns how many fares the taxi can hold at its current capacity level.
     *
     * @return the number of seats.
     */
    public int getCapacity() {
        return CAPACITY_BY_LEVEL[clampUpgrade(capacity_upgrade)];
    }

    /**
     * Returns the capacity upgrade level.
     *
     * @return the number of capacity upgrades bought.
     */
    public int getCapacityUpgrade() {
        return capacity_upgrade;
    }

    /**
     * Sets the capacity upgrade level, clamped to the legal range.
     *
     * @param upgrade the level to set
     */
    public void setCapacityUpgrade(int upgrade) {
        this.capacity_upgrade = clampUpgrade(upgrade);
    }

    /**
     * Returns how many seats a given capacity level provides.
     *
     * @param level the capacity upgrade level
     * @return the number of seats at that level.
     */
    public static int capacityAtLevel(int level) {
        return CAPACITY_BY_LEVEL[clampUpgrade(level)];
    }

    /**
     * Reports whether there is any room left.
     *
     * @return {@code true} when every seat is taken.
     */
    public boolean isFull() {
        return onBoard.size() >= getCapacity();
    }

    /**
     * Returns the money riding in the back, valued at what it would pay right now.
     *
     * <p>
     * Deliberately live rather than metered: the number the player watches in the HUD is what
     * stopping <em>this instant</em> is worth, which is exactly the decision they are making.
     * </p>
     *
     * @return the total the fares aboard would hand over if let out now.
     */
    public double getUnbankedTotal() {
        double total = 0;
        for (Ride ride : onBoard) {
            total += ride.getPayout();
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Career state
    // ------------------------------------------------------------------

    /**
     * Returns the lifetime count of pedestrians the taxi has run down.
     *
     * @return the career slimed total.
     */
    public int getCareerSlimed() {
        return career_slimed;
    }

    /**
     * Sets the lifetime count of pedestrians run down.
     *
     * @param slimed the career total to restore from a save file
     */
    public void setCareerSlimed(int slimed) {
        this.career_slimed = Math.max(0, slimed);
    }

    /**
     * Records another pedestrian who did not get out of the way.
     */
    public void addSlimed() {
        this.career_slimed++;
    }

    /**
     * Sets the EngineUpgrade of the taxi, clamped to the legal range.
     *
     * @param upgrade The EngineUpgrade of the taxi
     */
    public void setEngineUpgrade(int upgrade) {
        this.engine_upgrade = clampUpgrade(upgrade);
    }

    /**
     * Returns the EngineUpgrade of the taxi.
     *
     * @return The EngineUpgrade of the taxi
     */
    public int getEngineUpgrade() {
        return this.engine_upgrade;
    }

    /**
     * Returns the PotHoleResistance of the Taxi
     *
     * @return The PotHoleResistance of the Taxi
     */
    public int getPotholeResistance() {
        return this.pothole_resistance;
    }

    /**
     * Sets the PotHoleResistance of the Taxi, clamped to the legal range.
     *
     * @param resistance the number of wheel upgrades bought
     */
    public void setPotHoleResistance(int resistance) {
        this.pothole_resistance = clampUpgrade(resistance);
    }

    /**
     * Returns the pothole resistance that collisions should actually use.
     *
     * <p>
     * While NOS is firing the taxi shrugs potholes off entirely. That is expressed here
     * rather than by writing a temporary value into {@link #pothole_resistance}, so the
     * boost can never be mistaken for a purchased upgrade.
     * </p>
     *
     * @return the effective resistance for collision punishment purposes.
     */
    public int getEffectivePotholeResistance() {
        return nosActive ? MAX_UPGRADE + 1 : pothole_resistance;
    }

    /**
     * Reports whether every available upgrade has been bought.
     *
     * @return {@code true} once the engine and wheels are maxed out and NOS has been bought.
     */
    public boolean isFullyUpgraded() {
        return engine_upgrade >= MAX_UPGRADE
                && pothole_resistance >= MAX_UPGRADE
                && capacity_upgrade >= MAX_UPGRADE
                && hasNOS;
    }

    /**
     * Checks whether the taxi is carrying an unused canister of NOS.
     *
     * @return True if the taxi has NOS, false otherwise
     */
    public boolean hasNOS() {
        return this.hasNOS;
    }

    /**
     * Sets whether the taxi is carrying an unused canister of NOS.
     *
     * @param nos True if the taxi has NOS, false otherwise
     */
    public void setNOS(boolean nos) {
        this.hasNOS = nos;
    }

    /**
     * Returns whether NOS is currently firing.
     *
     * @return True if the boost is active, false otherwise
     */
    public boolean isNosActive() {
        return nosActive;
    }

    /**
     * Fires the NOS canister, consuming it.
     *
     * <p>
     * The canister is spent the moment it is used rather than when the boost expires, so
     * being caught mid-boost cannot hand the player a free refill.
     * </p>
     *
     * @return {@code true} if a boost was started, {@code false} if there was nothing to fire.
     */
    public boolean activateNos() {
        if (!hasNOS || nosActive) {
            return false;
        }
        hasNOS = false;
        nosActive = true;
        return true;
    }

    /**
     * Ends an in-progress NOS boost.
     */
    public void deactivateNos() {
        this.nosActive = false;
    }

    /**
     * Sets the image set of the Taxi.
     *
     * @param prime     The first image of the taxi
     * @param secondary The second image of the taxi
     */
    public void setImageSet(Image prime, Image secondary) {
        this.primeImage = prime;
        this.secondImage = secondary;
        // Show a frame immediately; otherwise the taxi is invisible until the first swap.
        this.isPrime = true;
        setImage(prime);
    }

    /**
     * Adds punishment to the taxi, this will slow it down.
     *
     * @param punishment The punishment time to add
     */
    public void addPunishment(int punishment) {
        this.punishment_clock += punishment;
    }

    /**
     * Returns the time of the taxi's punishment.
     *
     * @return The time of the taxi's punishment
     */
    public int getPunishment() {
        return this.punishment_clock;
    }

    /**
     * Reduces the punishment time of the taxi; a better engine shakes off a pothole faster.
     */
    public void minusPunishment() {
        this.punishment_clock = Math.max(0, this.punishment_clock - (1 + this.engine_upgrade));
    }

    /**
     * Returns the current {@code ETaxiPositions} of the taxi.
     *
     * @return the lane the taxi currently occupies.
     */
    public ETaxiPositions getPosition() {
        return myPosition;
    }

    /**
     * Adds cash to the Taxi's wallet without counting a fare.
     *
     * @param cash The cash to add to the wallet.
     */
    public void addCash(double cash) {
        wallet += cash;
    }

    /**
     * Returns the career total passengers.
     *
     * @return the number of total passengers collected in this save.
     */
    public int getCareerPassengers() {
        return this.career_passengers;
    }

    /**
     * Setting the career passengers of the taxi.
     *
     * @param career_passengers count of passengers
     */
    public void setCareerPassengers(int career_passengers) {
        this.career_passengers = Math.max(0, career_passengers);
    }

    /**
     * Returns the wallet of the taxi.
     *
     * @return The wallet of the taxi.
     */
    public double getWallet() {
        return this.wallet;
    }

    /**
     * Sets the wallet of the taxi.
     *
     * @param d wallet to set (amount)
     */
    public void setWallet(double d) {
        this.wallet = Double.isFinite(d) ? Math.max(0, d) : 0;
    }

    /**
     * Acceptor for the visitor pattern.
     */
    @Override
    public void accept(Visitor visitor) {
        // The taxi is the thing doing the colliding, so it never needs to be visited.
    }

    /**
     * Swaps the images of the taxi, used to make it look like the taxi's wheels are moving.
     */
    public void swapImage() {
        isPrime = !isPrime;
        setImage(isPrime ? primeImage : secondImage);
    }

    // ------------------------------------------------------------------
    // Presentation
    // ------------------------------------------------------------------

    /**
     * Advances everything that is purely cosmetic: the suspension, the mess on the paint and
     * the phase the seat lights pulse on.
     *
     * <p>
     * Kept separate from the game rules so that pausing freezes the picture without any of
     * it having to be unwound afterwards.
     * </p>
     */
    public void tickPresentation() {
        pulse++;

        // A critically-ish damped spring. Cheap, and it settles rather than ringing.
        bobVelocity += -bob * BOB_STIFFNESS - bobVelocity * BOB_DAMPING;
        bob += bobVelocity;
        if (Math.abs(bob) < 0.05 && Math.abs(bobVelocity) < 0.05) {
            bob = 0;
            bobVelocity = 0;
        }

        for (int i = gore.size() - 1; i >= 0; i--) {
            if (--gore.get(i).life <= 0) {
                gore.remove(i);
            }
        }
    }

    /**
     * Rocks the body on its springs.
     *
     * @param impulse how hard, in pixels per step; positive presses the nose down
     */
    public void nudge(double impulse) {
        bobVelocity += impulse;
    }

    /**
     * Throws a fresh coat of somebody across the front of the taxi.
     *
     * <p>
     * Stored in taxi-relative coordinates rather than screen coordinates, so the mess rides
     * with the vehicle through lane changes and the four different drawn sizes instead of
     * being left hanging in the air where the impact happened.
     * </p>
     */
    public void splatterFront() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (int i = 0; i < GORE_SPOTS; i++) {
            GoreSpot spot = new GoreSpot();

            // Two thirds hits the nose and the windscreen; the rest is dragged back down the
            // flank by the airflow, which is what stops it reading as a decal stuck on.
            if (i % 3 == 0) {
                spot.u = rng.nextDouble(0.30, 0.68);
                spot.v = rng.nextDouble(0.30, 0.70);
                spot.radius = rng.nextDouble(0.010, 0.028);
            } else {
                spot.u = rng.nextDouble(0.66, 0.99);
                spot.v = rng.nextDouble(0.24, 0.72);
                spot.radius = rng.nextDouble(0.016, 0.046);
            }

            spot.stretch = rng.nextDouble(1.0, 2.2);
            spot.drip = rng.nextDouble(0.0, 0.11);
            spot.dark = rng.nextBoolean();
            spot.life = GORE_LIFE_STEPS - rng.nextInt(40);
            spot.maxLife = spot.life;
            gore.add(spot);
        }

        while (gore.size() > MAX_GORE) {
            gore.remove(0);
        }

        // The impact drives the nose down before the springs push it back up.
        nudge(2.6);
    }

    /**
     * Reports whether there is anything unpleasant on the paintwork.
     *
     * @return {@code true} while a splatter is still showing.
     */
    public boolean isBloodied() {
        return !gore.isEmpty();
    }

    /**
     * Draws the taxi, the fares riding in it, and whatever is currently stuck to the front.
     *
     * <p>
     * Rather than editing the sprite sheet, each fare is drawn as a cropped head and
     * shoulders positioned in one of the minibus's window panes, sitting on a small lamp
     * whose colour says how near that fare is to where they asked to go. That keeps a single
     * taxi image working at all four lane sizes, and means a taxi full of fares who are ready
     * to get out is something the player can see rather than something they have to read.
     * </p>
     */
    @Override
    public void draw(GraphicsContext gc) {
        if (myImage == null) {
            return;
        }

        double taxiWidth = renderWidth();
        double taxiHeight = renderHeight();
        double drawY = y + bob;

        gc.drawImage(myImage, x, drawY, taxiWidth, taxiHeight);

        drawOccupants(gc, taxiWidth, taxiHeight, drawY);
        drawGore(gc, taxiWidth, taxiHeight, drawY);
    }

    /**
     * Draws one head per fare aboard, lit by how their trip is going.
     *
     * @param gc         the graphics context
     * @param taxiWidth  the taxi's drawn width
     * @param taxiHeight the taxi's drawn height
     * @param drawY      the taxi's drawn top edge, including the suspension bob
     */
    private void drawOccupants(GraphicsContext gc, double taxiWidth, double taxiHeight,
            double drawY) {
        if (occupantImage == null || onBoard.isEmpty()) {
            return;
        }

        // Seats are spread across the window band according to the taxi's capacity, so a
        // full taxi always looks full whatever it has been upgraded to, and the player can
        // read their load off the sprite instead of off the counter.
        int capacity = getCapacity();
        double bandWidth = taxiWidth * SEAT_BAND_WIDTH;
        double step = bandWidth / capacity;
        double seatWidth = Math.min(step * 0.92, taxiWidth * SEAT_MAX_WIDTH);
        double seatHeight = taxiHeight * SEAT_HEIGHT;
        double sourceHeight = occupantImage.getHeight() * PASSENGER_HEAD_FRACTION;
        double seatY = drawY + taxiHeight * SEAT_TOP_Y;

        double flash = 0.5 + 0.5 * Math.sin(pulse * 0.22);

        gc.save();
        for (int seat = 0; seat < onBoard.size(); seat++) {
            Ride ride = onBoard.get(seat);
            double seatX = x + taxiWidth * SEAT_BAND_X + seat * step;
            Color lamp = gradeColour(ride.getGrade());

            // A fare who is ready to get out flashes; everybody else sits quietly lit, so the
            // eye is drawn only to the seats that are actually worth stopping for.
            double glow = ride.getGrade() == Ride.Grade.ON_THE_DOT ? 0.45 + 0.55 * flash : 0.34;

            gc.setGlobalAlpha(glow);
            gc.setFill(lamp);
            gc.fillRoundRect(seatX - seatWidth * 0.30, seatY - seatHeight * 0.18,
                    seatWidth * 1.60, seatHeight * 1.36, seatWidth, seatWidth);

            // Riders sit behind tinted glass, so they read as inside the vehicle.
            gc.setGlobalAlpha(0.9);
            gc.drawImage(occupantImage,
                    0, 0, occupantImage.getWidth(), sourceHeight,
                    seatX, seatY, seatWidth, seatHeight);
        }
        gc.restore();
        gc.setGlobalAlpha(1.0);
    }

    /**
     * Draws whatever is currently drying on the bodywork.
     *
     * @param gc         the graphics context
     * @param taxiWidth  the taxi's drawn width
     * @param taxiHeight the taxi's drawn height
     * @param drawY      the taxi's drawn top edge, including the suspension bob
     */
    private void drawGore(GraphicsContext gc, double taxiWidth, double taxiHeight, double drawY) {
        if (gore.isEmpty()) {
            return;
        }

        gc.save();
        for (GoreSpot spot : gore) {
            // Holds full strength while it is wet, then dries off over the last second and a
            // half rather than vanishing between one frame and the next.
            double fade = Math.min(1.0, spot.life / (double) GORE_FADE_STEPS);
            double aged = spot.life / (double) spot.maxLife;

            double blobWidth = taxiWidth * spot.radius * spot.stretch;
            double blobHeight = taxiWidth * spot.radius;
            double bx = x + taxiWidth * spot.u - blobWidth / 2;
            double by = drawY + taxiHeight * spot.v - blobHeight / 2;

            gc.setGlobalAlpha(0.88 * fade);
            gc.setFill(spot.dark ? Color.web("#5e0209") : Color.web("#96101a"));
            gc.fillOval(bx, by, blobWidth, blobHeight);

            // Runs of it crawl down the panel as the paint dries.
            if (spot.drip > 0.005) {
                double run = taxiHeight * spot.drip * (1.0 - aged);
                gc.setGlobalAlpha(0.6 * fade);
                gc.fillRoundRect(bx + blobWidth * 0.36, by + blobHeight * 0.4,
                        blobWidth * 0.28, blobHeight * 0.4 + run,
                        blobWidth * 0.28, blobWidth * 0.28);
            }
        }
        gc.restore();
        gc.setGlobalAlpha(1.0);
    }

    /**
     * Returns the colour a trip is shown in.
     *
     * @param grade how the trip is going
     * @return the lamp colour for that grade.
     */
    public static Color gradeColour(Ride.Grade grade) {
        switch (grade) {
            case ON_THE_DOT:
                return Color.web("#3ef07a");
            case NEARLY:
                return Color.web("#ffd166");
            case OVERSHOT:
                return Color.web("#ff6b6b");
            case EARLY:
            default:
                return Color.web("#4dd4ff");
        }
    }

    /**
     * Scales the taxi based on the given {@code ETaxiPositions}.
     *
     * <p>
     * The bounding box is rebuilt from scratch on every call (rather than nudged) so that
     * repeated lane changes cannot drift it out of alignment with the sprite.
     * </p>
     *
     * @param position The {@code ETaxiPositions} to scale the taxi to.
     */
    public void scale(ETaxiPositions position) {
        myPosition = position;
        double height = position.getHeightScale();

        this.setY(position.getLocation());
        fitToHeight(height);
        centreOnRoad();

        // The bound box has to track the lane: sprites further "up" the road are drawn
        // smaller and higher, and a fixed box would collide with the neighbouring lane.
        this.myBound.setWidth(height * 1.5);
        this.myBound.setY(this.myBound.getY() + 30);
        this.myBound.setHeight(height * 0.5);

        switch (position) {
            case FIRST_LANE_TAXI:
                this.myBound.setY(this.myBound.getY() - 20);
                break;
            case SECOND_LANE_TAXI:
                this.myBound.setY(this.myBound.getY() + 40);
                this.myBound.setHeight(this.myBound.getHeight() - 30);
                break;
            case THIRD_LANE_TAXI:
                this.myBound.setY(this.myBound.getY() + 50);
                this.myBound.setHeight(this.myBound.getHeight() - 35);
                break;
            case FOURTH_LANE_TAXI:
                this.myBound.setY(this.myBound.getY() + 60);
                this.myBound.setHeight(this.myBound.getHeight() - 40);
                break;
        }
    }

    /**
     * Parks the taxi in the middle of the screen.
     *
     * <p>
     * The taxi holds a fixed horizontal position and the world moves past it, so the only
     * driving decision is which lane to be in. The taxi is drawn at four different sizes,
     * one per lane, so the centred position has to be recomputed whenever it is scaled —
     * otherwise the larger near-lane sprite would sit noticeably right of centre.
     * </p>
     */
    public void centreOnRoad() {
        double width = renderWidth();
        if (width <= 0) {
            return;
        }
        setX((int) Math.round((ESettings.SCENE_WIDTH.getVal() - width) / 2.0));
    }

    /**
     * Keeps an upgrade level inside the range the game supports.
     *
     * @param value the requested level, possibly from a hand-edited save file.
     * @return the level clamped to {@code 0..}{@value #MAX_UPGRADE}.
     */
    private static int clampUpgrade(int value) {
        return Math.min(MAX_UPGRADE, Math.max(0, value));
    }

    /** One blob of somebody, stuck to the bodywork in taxi-relative coordinates. */
    private static final class GoreSpot {
        /** Position across the taxi, 0 at the back and 1 at the nose. */
        double u;
        /** Position down the taxi, 0 at the roof and 1 at the sills. */
        double v;
        /** Blob size, as a fraction of the taxi's drawn width. */
        double radius;
        /** How far the blob is smeared horizontally by the airflow. */
        double stretch;
        /** How far it runs down the panel, as a fraction of the taxi's height. */
        double drip;
        boolean dark;
        int life;
        int maxLife;
    }
}
