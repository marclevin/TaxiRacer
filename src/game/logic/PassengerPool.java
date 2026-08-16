package game.logic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;

import game.display.models.Passenger;
import game.utility.EPassenger;
import game.utility.ESettings;
import javafx.scene.image.Image;

/**
 * An object pool for passengers.
 *
 * <p>
 * Passengers are created and dropped constantly during a run, so collected ones are parked
 * here and handed back out with a fresh pavement, position and fare instead of being
 * reallocated.
 * </p>
 */
public final class PassengerPool {

    private static PassengerPool instance = null;
    private static Image passengerImage = null;

    private final Deque<Passenger> idle = new ArrayDeque<>();

    /**
     * Private constructor to ensure only one instance exists.
     */
    private PassengerPool() {
    }

    /**
     * This function returns the instance of the passenger pool.
     *
     * @return The instance of the passenger pool
     */
    public static PassengerPool getInstance() {
        if (instance == null) {
            instance = new PassengerPool();
        }
        return instance;
    }

    /**
     * This function sets the image of passengers created in the pool.
     *
     * @param image The image of the passengers.
     */
    public static void setPassengerImage(Image image) {
        passengerImage = image;
    }

    /**
     * Hands out a passenger, recycling a collected one where possible.
     *
     * <p>
     * Every passenger is re-randomised on the way out — pavement, position, destination and
     * fare — so a recycled instance is indistinguishable from a fresh one.
     * </p>
     *
     * @return a ready-to-use passenger.
     */
    public Passenger acquirePassenger() {
        Passenger p = idle.pollLast();
        if (p == null) {
            p = new Passenger(0, 0);
        }

        p.setImage(passengerImage);
        // scale() also fixes the Y position, so it has to run before the X is chosen.
        p.scale(ThreadLocalRandom.current().nextBoolean()
                ? EPassenger.PASSENGER_BOTTOM
                : EPassenger.PASSENGER_TOP);
        p.setX(randomX());
        p.roll();
        return p;
    }

    /**
     * Releases a passenger back to the pool.
     *
     * @param p The passenger to be released.
     */
    public void releasePassenger(Passenger p) {
        if (p != null) {
            idle.addLast(p);
        }
    }

    /**
     * Empties the pool, so a new game does not inherit the previous game's passengers.
     */
    public void clear() {
        idle.clear();
    }

    /**
     * This function gets a random X position for a passenger.
     *
     * <p>
     * The band is twice the screen width because sprites scroll left and wrap around, so
     * spawning off screen to the right is what staggers them naturally.
     * </p>
     *
     * @return A random X position for a passenger.
     */
    private static int randomX() {
        int width = ESettings.SCENE_WIDTH.getVal();
        return ThreadLocalRandom.current().nextInt(-width, width);
    }
}
